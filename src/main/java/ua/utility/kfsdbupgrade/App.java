/*
 * Copyright 2014 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl2.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ua.utility.kfsdbupgrade;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import liquibase.FileSystemFileOpener;
import liquibase.Liquibase;
import ua.utility.kfsdbupgrade.mdoc.DataLoader;
import ua.utility.kfsdbupgrade.mdoc.DataPumper;
import ua.utility.kfsdbupgrade.mdoc.ExecutorProvider;
import ua.utility.kfsdbupgrade.mdoc.MaintDocConverter;
import ua.utility.kfsdbupgrade.mdoc.MaintDocResult;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.ThreadsProvider;

public class App {
	private static final Logger LOGGER = Logger.getLogger(App.class);

    private static final int MAINTENANCE_DOCUMENT_UPDATE_BATCH_SIZE = 1000;
    
	public static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final String UNDERLINE = "--------------------------------------------------------------------------------------------------------------------------";
    public static final String ERROR = "************************************************* error *************************************************";
    public static final String HEADER1 = "================================================ ? ================================================";
    private static final String INDEX_NAME_TEMPLATE = "[table-name]I{index}";

	private static final String MISC_SQL_PATH = "sql/misc.sql";
	protected static final String KFS_INDEXES_SQL_PATH = "sql/kfs-indexes.sql";
	private static final String KFS_PUBLIC_SYNONYMS_SQL_PATH = "sql/kfs-public-synonyms.sql";
	protected static final String DEFAULT_PROPERTIES_FILE = "src/main/resources/kfsdbupgrade.properties";
	/**
	 * Populated by the <code>upgrade-base-directory</code> {@link Properties}
	 * entry
	 */
    private static String upgradeRoot;

	/**
	 * Populated by the <code>post-upgrade-directory</code> {@link Properties}
	 * entry if set, otherwise defaults to {@link #upgradeRoot}
	 * <code>/post-upgrade/sql</code>.
	 */
	private final File postUpgradeDirectory;
	/**
	 * {@link List} of {@link String} file paths loaded from the {@link App}'s
	 * {@link Properties} <code>upgrade-folders</code> property.
	 */
    private List<String> upgradeFolders;
    private Properties properties;
	/**
	 * {@link Map} of {@link String} keys representing a directory containing a
	 * {@link List} of {@link String} names of files to process
	 */
    private Map<String, List<String>> upgradeFiles;

	/**
	 * {@link Set} of post-upgrade {@link File}s that were processed. At end of
	 * processing, should check if there are any files in the post-processing
	 * directory that were not configured to run.
	 */
	private final Set<File> postUpgradeFilesProcessed = new HashSet<File>();
	
  /**
   * Main program entry point. Single argument is expected of a path to the <code>kfsdbupgrade.properties</code> properties file. Optional second argument of
   * "<code>ingestWorkflow</code>" if the ingest workflow code path should be followed instead of the database upgrade code path.
   * 
   * @param args
   */
  public static void main(final String args[]) {
     if (parseBoolean(System.getProperty("mdoc.load"))) {
        try {
          doLoad();
          System.exit(0);
        } catch (Throwable e) {
          e.printStackTrace();
          LOGGER.error("unexpected loading error", e);
          System.exit(1);
        }
      }
      if (parseBoolean(System.getProperty("mdoc.metrics"))) {
        try {
          doMetrics();
          System.exit(0);
        }catch(Throwable e) {
          e.printStackTrace();
          LOGGER.error("unexpected metrics error", e);
          System.exit(1);
        }
      }
      
        if (args.length > 0) {
            String propertyFileName = args[0];
            boolean ingestWorkflow = false;
            if (args.length > 1) {
                ingestWorkflow = "ingestWorkflow".equalsIgnoreCase(args[1]);
            }
			App app = new App(propertyFileName);
			if (ingestWorkflow) {
				app.doWorkflow(propertyFileName);
			} else {
				app.doUpgrade();
			}
		}
    }
    
    private static void doLoad() throws IOException {
      File defaultFile = new File("./kfsdbupgrade.properties").getCanonicalFile();
      File actualFile = new File(System.getProperty("upgrade.props", defaultFile.getPath())).getCanonicalFile();
      Properties props = new PropertiesProvider(actualFile).get();
      DataLoader loader = new DataLoader(props);
      loader.get();
    }

    private static void doMetrics() throws IOException {
      File defaultFile = new File("./kfsdbupgrade.properties").getCanonicalFile();
      File actualFile = new File(System.getProperty("upgrade.props", defaultFile.getPath())).getCanonicalFile();
      Properties props = new PropertiesProvider(actualFile).get();
      DataPumper pumper = new DataPumper(props);
      pumper.get();
    }

    /**
	 * Constructor.
	 * 
	 * @param propertyFileName
	 *            {@link String} of the <code>.properties</code> file location
	 *            to use
	 */
	public App(String propertyFileName) {
		properties = loadProperties(DEFAULT_PROPERTIES_FILE);
        if (propertyFileName != null) {
			Properties overridenProps = loadProperties(propertyFileName);
			if ( overridenProps != null ){
				properties.putAll(overridenProps);
			}
			LOGGER.debug("Finished loading properties from "+propertyFileName);
            upgradeRoot = properties.getProperty("upgrade-base-directory");
			/*
			 * If the post-upgrade-directory property is specified, use it as
			 * the path for the directory; otherwise, default to
			 * {upgrade-root}/post-upgrade/sql
			 */
			String postUpgradeDirectoryProperty = properties.getProperty("post-upgrade-directory");
			if (postUpgradeDirectoryProperty != null) {
				postUpgradeDirectory = new File(postUpgradeDirectoryProperty);
			} else {
				postUpgradeDirectory = new File(upgradeRoot + "/post-upgrade");
			}
            upgradeFolders = loadList(properties.getProperty("upgrade-folders"));
            upgradeFiles = loadFolderFileMap("files-");
			Appender logFileAppender;
			try {
				logFileAppender = new FileAppender(new SimpleLayout(), properties.getProperty("output-log-file-name"));
				LOGGER.addAppender(logFileAppender);
			} catch (IOException e) {
				/*
				 * Unable to recover, but still logging to console, so
				 * reasonable to continue
				 */
				LOGGER.error("Unable to log to file " + properties.getProperty("output-log-file-name")
						+ " . IOException encountered: ", e);
			}
        } else {
			LOGGER.fatal("invalid properties file: " + propertyFileName);
			postUpgradeDirectory = null;
        }
    }
            
	/**
	 * Main entry point for the database upgrade code path.
	 */
    private void doUpgrade() {
    	/*
		 * conn1 used for miscellanous SQL statements and dropping temp tables,
		 * etc., and has autocommit set to 'true'. conn2 is used by liquibase
		 */
        Connection conn1 = null;
        Connection conn2 = null;
        Statement stmt = null;
        boolean success = false;
        try {
            conn1 = getUpgradeConnection();
            conn2 = getUpgradeConnection();
            conn2.setAutoCommit(true);
            stmt = conn2.createStatement();
            stmt.execute("ALTER SESSION ENABLE PARALLEL DML");
            stmt.close();
            stmt = conn1.createStatement();
			LOGGER.info("Starting KFS database upgrade process...");
			
			      if (parseBoolean(System.getProperty("mdoc.only"))) {
			        LOGGER.info("Converting maintenance documents only");
			        convertMaintenanceDocuments(conn1);
			        return;
			      }

            if (doInitialProcessing(conn1, stmt)) {
                doCommit(conn1);
                if (doUpgrade(conn1, conn2, stmt)) {
                    success = true;
                }
            }

            if (success) {
                stmt.close();
                stmt = conn2.createStatement();
                try {
					dropTempTables(conn2, stmt);
				} catch (Exception e) {
					LOGGER.error("dropTempTables(conn2, stmt); -- FAILED in doUpgrade()", e);
				}
				try {
					runMiscSql(conn2, stmt);
				} catch (Exception e) {
					LOGGER.error("runMiscSql(conn2, stmt); -- FAILED in doUpgrade()", e);
				}
				try {
					populateProcurementCardTable(conn1);
				} catch (Exception e) {
					LOGGER.error("populateProcurementCardTable(conn1); -- FAILED in doUpgrade() ", e);
				}
				try {
					updatePurchasingStatuses(conn1);
				} catch (Exception e) {
					LOGGER.error("updatePurchasingStatuses(conn1); -- FAILED in doUpgrade() ", e);
				}
				try {
					File kfsIndexesSqlFile = new File(postUpgradeDirectory + File.separator + KFS_INDEXES_SQL_PATH);
					createExistingIndexes(conn2, stmt, kfsIndexesSqlFile);
				} catch (Exception e) {
					LOGGER.error("createExistingIndexes(conn2, stmt); -- FAILED in doUpgrade() ", e);
				}
				try {
					createPublicSynonyms(conn2, stmt);
				} catch (Exception e) {
					LOGGER.error("createPublicSynonyms(conn2, stmt); -- FAILED in doUpgrade() ", e);
				}
				try {
					createForeignKeyIndexes(conn2, stmt);
				} catch (Exception e) {
					LOGGER.error("createForeignKeyIndexes(conn2, stmt) -- FAILED in doUpgrade() ", e);
				}
				try {
					createDocumentSearchEntries(conn2, stmt);
				} catch (Exception e) {
					LOGGER.error("createDocumentSearchEntries(conn2, stmt); -- FAILED in doUpgrade()", e);
				}
				if (StringUtils.equalsIgnoreCase(properties.getProperty("run-maintenance-document-conversion"), "true")) {
                    try {
						convertMaintenanceDocuments(conn1);
					} catch (Exception e) {
						LOGGER.error("convertMaintenanceDocuments(conn1); -- FAILED in doUpgrade() ", e);
					}
                }
                logHeader1("upgrade completed successfully");
            }
			/*
			 * In default configuration, post-upgrade directory has a child
			 * directory of 'sql', and the entries in 'files-post-upgrade' all
			 * prepend 'sql/' to their path. So, have to replicate that logic
			 * here before calculating if there are any missing files.
			 */
			File sqlSubdirectory = postUpgradeDirectory.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {

					return pathname.getName().equals("sql");

				}
			})[0];
			Set<File> unprocessedPostUpgradeFiles = getUnprocessedFiles(sqlSubdirectory,
					postUpgradeFilesProcessed);
			for (File unprocessedFile : unprocessedPostUpgradeFiles) {
				LOGGER.warn("The file " + unprocessedFile.getAbsolutePath()
						+ " in the post-upgrade directory was not processed.");
			}
		}

        catch (Exception ex) {
			LOGGER.fatal(ex);
        } 

        finally {
            closeDbObjects(conn1, stmt, null);
            closeDbObjects(conn2, null, null);
        }
    }
    
	/**
	 * Main entry point for the workflow ingestion code path
	 * 
	 * @param propertyFileName
	 *            {@link String} of the path to the <code>.properties</code>
	 *            file to use
	 */
    private void doWorkflow(String propertyFileName) {
        System.getProperties().setProperty("security.property.file", "file:" + propertyFileName);
		new WorkflowImporter(upgradeRoot, upgradeFolders);
    }
    
	/**
	 * @param input
	 *            {@link String} of a comma-separated list of values
	 * @return {@link List} of the comma-separated {@link String}s
	 */
    private List<String> loadList(String input) {
        List<String> retval = new ArrayList<String>();
        if (StringUtils.isNotBlank(input)) {
            StringTokenizer st = new StringTokenizer(input, ",");

            while (st.hasMoreTokens()) {
                retval.add(st.nextToken().trim());
            }
        }
        return retval;
    }

	/**
	 * Drops <code>user_tables</code> with a table name that begins with
	 * <code>OLD_</code> or <code>TEMP_</code>
	 * 
	 * @param conn
	 *            Unused and should be removed
	 * 
	 * @param stmt
	 *            {@link Statement} to use to execute the SQL query.
	 */
    private void dropTempTables(Connection conn, Statement stmt) {
        logHeader2("Dropping temporary tables");

        List<String> tables = new ArrayList<String>();
        ResultSet res = null;
        try {
            res = stmt.executeQuery("select table_name from user_tables where table_name like 'OLD_%' or table_name like 'TEMP_%'");

            while (res.next()) {
                tables.add(res.getString(1));
            }

            for (String t : tables) {
                try {
                    stmt.execute("drop table " + t);
                } catch (Exception ex) {
					LOGGER.error("failed to drop temp table " + t, ex);
                }
            }
        } catch (Exception ex) {
			LOGGER.error(ex);
        } finally {
            closeDbObjects(null, null, res);
        }
    }

	/**
	 * From this {@link App}'s {@link Properties}, gets entries with keys that
	 * are directory names starting with <code>prefix</code>, loads the
	 * {@link Properties} values as a {@link List} via {@link #loadList(String)}
	 * , and returns the values as a {@link Map} with keys of the matched
	 * directory names to a {@link List} of files in that directory.
	 * 
	 * @param prefix
	 *            {@link String} of the prefix to match on this {@link App}'s
	 *            {@link Properties} entries
	 * @return {@link Map} with keys of the matched directory names from this
	 *         {@link App}'s {@link Properties} to a {@link List} of files in
	 *         that directory.
	 */
    private Map<String, List<String>> loadFolderFileMap(String prefix) {
        Map<String, List<String>> retval = new HashMap<String, List<String>>();

		for (Entry<Object, Object> e : properties.entrySet()) {
            String key = (String) e.getKey();
            if (key.startsWith(prefix)) {
                String folder = key.substring(prefix.length());
                retval.put(folder, loadList((String) e.getValue()));
            }
        }

        return retval;
    }

	/**
	 * @param fname
	 *            {@link String} location of a file to load into
	 *            {@link Properties}
	 * @return {@link Properties} loaded from the file at <code>fname</code>, or
	 *         <code>null</code> if the file at <code>fname</code> does not
	 *         contain an entry for <code>database-url</code> or if an
	 *         {@link Exception} is encountered while attempting to read the
	 *         file.
	 */
    private Properties loadProperties(String fname) {
        Properties properties = new Properties();
        FileReader reader = null;

        try {
            reader = new FileReader(fname);
			properties.load(reader);
        } catch (Exception ex) {
			LOGGER.error(ex);
			properties = null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception ex) {
            }
        }

        return properties;
    }

	/**
	 * Calls {@link Connection#rollback()}, and if any {@link Exception}s are
	 * encountered redirects them to {@link #writeLog(Exception)}.
	 * 
	 * @param conn
	 *            {@link Connection} to rollback.
	 */
    protected void doRollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception ex) {
			LOGGER.error(ex);
        }
    }

	/**
	 * Calls {@link Connection#commit()}, and if any {@link Exception}s are
	 * encountered redirects them to {@link #writeLog(Exception)}.
	 * 
	 * @param conn
	 *            {@link Connection} to commit.
	 * @return <code>true</code> if no {@link Exception}s were encountered while
	 *         committing, <code>false</code> otherwise.
	 */
    private boolean doCommit(Connection conn) {
        boolean retval = true;
        try {
            conn.commit();
        } catch (Exception ex) {
			LOGGER.error(ex);
            retval = false;
        }

        return retval;
    }

	/**
	 * Execute the SQL statements in the provided {@link File}
	 * 
	 * @param conn
	 *            {@link Connection} that SQL statements will be executed over
	 * @param stmt
	 *            {@link Statement} to use to execute SQL statements
	 * @param f
	 *            {@link File}
	 * @param delimiter
	 *            This parameter is not used and should be removed.
	 * @return <code>true</code> if all of the SQL statements in the
	 *         {@link File} were executed successfully, <code>false</code>
	 *         otherwise
	 */
    protected boolean runSqlFile(Connection conn, Statement stmt, File f, String delimiter) {
        boolean retval = true;
        logHeader2("processing sql file " + f.getPath());
        List<String> sqlStatements = getSqlStatements(f);

        if (!sqlStatements.isEmpty()) {
            for (String sql : sqlStatements) {
				LOGGER.info("Executing sql: " + sql);
				if (!executeSql(conn, stmt, sql)) {
                    retval = false;
                    break;
                }
            }

            if (retval) {
                retval = doCommit(conn);
            } else {
                doRollback(conn);
            }
        } else {
            retval = false;
			LOGGER.error("no sql statements found!");
        }
		postUpgradeFilesProcessed.add(f);
        return retval;
    }

	/**
	 * Read SQL statements from the provided {@link File} into a {@link List} of
	 * {@link String}s. Blank lines and comment lines (lines beginning with
	 * "<code>--</code>") are skipped.
	 * 
	 * @param f
	 *            {@link File} to read SQL statements from
	 * @return {@link List} of {@link String}s representing the SQL statements
	 *         to execute read from the provided {@link File}
	 */
    private List<String> getSqlStatements(File f) {
        List<String> retval = new ArrayList<String>();
        LineNumberReader lnr = null;

        try {
            lnr = new LineNumberReader(new FileReader(f));
            String line = null;
            StringBuilder sql = new StringBuilder(512);

            while ((line = lnr.readLine()) != null) {
                if (StringUtils.isNotBlank(line) && !line.trim().startsWith("--")) {
                    line = line.trim();
					// FIXME hardcoded delimiters
                    if (line.equals("/") || line.equals(";")) {
                        if (sql.length() > 0) {
                            retval.add(sql.toString());
                            sql.setLength(0);
                        }
                    } else if (line.endsWith("/") || line.endsWith(";")) {
                        sql.append(" ");
                        sql.append(line.substring(0, line.length() - 1));
                        retval.add(sql.toString());
                        sql.setLength(0);
                    } else {
                        sql.append(" ");
                        sql.append(line);
                    }
                }
            }

            if (sql.length() > 0) {
                retval.add(sql.toString());
            }
        } catch (Exception ex) {
			LOGGER.error(ex);
        } finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception ex) {
            }
        }

        return retval;
    }

	/**
	 * From the provided {@link File}, get a reference to the upgrade version of
	 * the file. In general, this means for the file <code>someFile.ext</code>,
	 * get a reference to the {@link File}
	 * <code>someFile<strong>_mod</strong>.ext</code>.
	 * 
	 * @param fname
	 *            {@link File} to get the upgrade version for
	 * @return {@link File} referencing the upgrade version of the provided
	 *         {@link File}
	 */
    private File getUpgradeFile(String fname) {
        File retval = null;

        int pos = fname.lastIndexOf(".");

        File modFile = new File(fname.substring(0, pos) + "_mod" + fname.substring(pos));

        if (modFile.isFile() && modFile.exists()) {
            retval = modFile;
        } else {
            retval = new File(fname);
        }

        return retval;
    }

	/**
	 * Extract the directory name from the <code>lastProcessedFile</code> file
	 * path
	 * 
	 * @param lastProcessedFile
	 *            {@link String} of the file path of the last processed file
	 * @return {@link String} of the directory name containing the last
	 *         processed file
	 */
	/*
	 * FIXME Use File.getParent() instead. No need to rely on an outside
	 * 'base-directory' property when all we're doing is basic file IO
	 */
	private String getLastProcessedFolder(String lastProcessedFile) {
        String retval = null;
        String s = lastProcessedFile.substring(properties.getProperty("upgrade-base-directory").length() + 1);
        int pos = s.indexOf("/");
        retval = s.substring(0, pos);
        return retval;
    }

	/**
	 * @param lastProcessedFile
	 *            {@link String} name of the last processed file, or the empty
	 *            {@link String} or <code>null</code> if this is a fresh run.
	 * @return If <code>lastProcessedFile</code> is specified, then will return
	 *         a {@link List} of {@link String}s of the directories that still
	 *         remain to be processed, which will be a subset of
	 *         {@link #upgradeFolders}; otherwise, will return a full copy of
	 *         {@link #upgradeFolders}
	 */
    private List<String> getFolders(String lastProcessedFile) {
        List<String> retval = new ArrayList<String>();

        if (StringUtils.isNotBlank(lastProcessedFile)) {
            String lastProcessedFolder = getLastProcessedFolder(lastProcessedFile);
            boolean foundit = false;
            for (String folder : upgradeFolders) {
                if (lastProcessedFolder.equals(folder)) {
                    foundit = true;
                }

                if (foundit) {
                    retval.add(folder);
                }
            }
        } else {
            retval = upgradeFolders;
        }

        return retval;
    }

	/**
	 * @param folder
	 *            {@link String} of the directory path to get the child
	 *            filenames to process
	 * @param lastProcessedFile
	 *            {@link String} name of the last processed file, or the empty
	 *            {@link String} or <code>null</code> if this is a fresh run.
	 *
	 * @return If <code>lastProcessedFile</code> is specified and exists in the
	 *         specified <code>folder</code>, will return a {@link List} of
	 *         {@link String}s of the files remaining in the <code>folder</code>
	 *         that still need to be processed; otherwise, will return the
	 *         {@link List} of {@link String}s in {@link #upgradeFiles} with the
	 *         key of <code>folder</code>.
	 */
	// FIXME use Java builtins for File IO. They're FREE
    private List<String> getFolderFiles(String folder, String lastProcessedFile) {
        List<String> retval = new ArrayList<String>();
        if (StringUtils.isBlank(lastProcessedFile)) {
            retval = upgradeFiles.get(folder);
        } else {
            boolean foundit = false;
            String lastProcessedFolder = getLastProcessedFolder(lastProcessedFile);
            int len = (properties.getProperty("upgrade-base-directory") + "/" + folder + "/").length();

            if (!folder.equals(lastProcessedFolder)) {
                retval = upgradeFiles.get(folder);
            } else {
                for (String s : upgradeFiles.get(folder)) {
                    if (s.equals(lastProcessedFile.substring(len))) {
                        foundit = true;
                    } else if (foundit) {
                        retval.add(s);
                    }
                }
            }
        }
		/*
		 * if getting for the post-upgrade directory, purge any "special" files
		 * that are hardcoded into methods
		 */
		if (folder.equals(postUpgradeDirectory.getName())) {
			if (retval.contains(MISC_SQL_PATH)) {
				LOGGER.warn("Manual configuration for " + MISC_SQL_PATH + " in " + postUpgradeDirectory.getName()
						+ ". This file is automatically picked up and executed"
						+ " and should not be in the configuration. Ignoring.");
				retval.remove(MISC_SQL_PATH);
			}
			if (retval.contains(KFS_INDEXES_SQL_PATH)) {
				LOGGER.warn("Manual configuration for " + KFS_INDEXES_SQL_PATH + " in " + postUpgradeDirectory.getName()
						+ ". This file is automatically picked up and executed"
						+ " and should not be in the configuration. Ignoring.");
				retval.remove(KFS_INDEXES_SQL_PATH);
			}
			if (retval.contains(KFS_PUBLIC_SYNONYMS_SQL_PATH)) {
				LOGGER.warn("Manual configuration for " + KFS_PUBLIC_SYNONYMS_SQL_PATH + " in "
						+ postUpgradeDirectory.getName() + ". This file is automatically picked up and executed"
						+ " and should not be in the configuration. Ignoring.");
				retval.remove(KFS_PUBLIC_SYNONYMS_SQL_PATH);
			}
		}
        return retval;
    }

	/**
	 * Main entry point for the workspace upgrade code path
	 * 
	 * @param conn1
	 *            {@link Connection} to use when executing custom SQL
	 * @param conn2
	 *            {@link Connection} to use when executing Liquibase
	 * @param stmt
	 *            {@link Statement} to use in conjuction with <code>conn1</code>
	 * @return <code>true</code> if all files processed correctly,
	 *         <code>false</code> otherwise
	 */
    private boolean doUpgrade(Connection conn1, Connection conn2, Statement stmt) {
        boolean retval = true;
        logHeader1("upgrading kfs");

        String lastProcessedFile = properties.getProperty("last-processed-file");
		RunRequest runRequest = buildRunRequest(lastProcessedFile);
		List<String> folders = runRequest.getDirectories();

        for (String folder : folders) {
            logHeader2("processing folder " + folder);

			List<String> folderFiles = runRequest.getFilesForDirectory(folder);
            if (folderFiles != null) {
                for (String fname : folderFiles) {
                    if (isMethodCall(fname)) {
                        retval = callMethod(fname, conn1, stmt);
                    } else {
                        File f = getUpgradeFile(upgradeRoot + "/" + folder + "/" + fname);

                        if (f.getName().endsWith(".sql")) {
                            if (!runSqlFile(conn1, stmt, f, ";")) {
                                retval = false;
                                writeProcessedFileInfo("[failure] " + f.getPath());
								doRollback(conn1);
								break;
                            } else {
                                writeProcessedFileInfo("[success] " + f.getPath());
								doCommit(conn1);
                            }
                        } else {
                            if (!runLiquibase(conn2, f)) {
                                retval = false;
                                writeProcessedFileInfo("[failure] " + f.getPath());
								doRollback(conn2);
								break;
                            } else {
                                writeProcessedFileInfo("[success] " + f.getPath());
								doCommit(conn2);
                            }
                        }
                    }
                }
            } else {
                retval = true;
            }

            if (!retval) {
                break;
            }
        }

        return retval;
    }

	/**
	 * @param lastProcessedFile
	 *            {@link String} name of the last file that was succesfully
	 *            processed
	 * @return {@link RunRequest} containing the directories and files to
	 *         process based on the provided <code>lastFileProcessed</code>
	 */
	public RunRequest buildRunRequest(String lastProcessedFile) {
		List<String> directories = getFolders(lastProcessedFile);
		Map<String, List<String>> directoriesToFiles = new HashMap<String, List<String>>();
		for (String dir : directories) {
			List<String> directoryFiles = getFolderFiles(dir, lastProcessedFile);
			directoriesToFiles.put(dir, directoryFiles);
		}
		return new RunRequest(directories, directoriesToFiles);
	}

	/**
	 * Wrapper method to execute call to Liquibase and handle any exceptions
	 * encountered
	 * 
	 * @param conn
	 *            {@link Connection} to use when executing Liquibase
	 * @param f
	 *            {@link File} representing a Liquibase changelog to execute
	 * @return <code>true</code> if the changelog {@link File} was successfully
	 *         processed, <code>false</code> otherwise.
	 */
    private boolean runLiquibase(Connection conn, File f) {
        boolean retval = true;
        logHeader2("processing liquibase file " + f.getPath());
        PrintWriter pw = null;
        try {
            Liquibase liquibase = new Liquibase(f.getName(), new FileSystemFileOpener(f.getParentFile().getPath()), conn);
            liquibase.getDatabase().setAutoCommit(true);
			// TODO will need to test to see how this interacts with log4j
			liquibase.reportStatus(true, null, pw = getOutputLogWriter());
            liquibase.update(null);
            retval = true;
        } catch (Exception ex) {
            retval = false;
            if (pw != null) {
                pw.close();
            }
            pw = null;
			LOGGER.error(ex);
        } finally {
            if (pw != null) {
                pw.close();
            }
        }

        return retval;
	}

	/**
	 * @return {@link String} of the current {@link Date} formatted in a way
	 *         suitable to use for logging.
	 */
   public String getTimeString() {
        return "[" + DF.format(new Date()) + "] ";
    }

	/**
	 * 
	 * @return {@link Connection} to the database with the URL of
	 *         <code>database-url</code> in this {@link App}s {@link Properties}
	 *         . Uses the <code>database-user</code>,
	 *         <code>database-password</code>, <code>database-name</code>, and
	 *         <code>database-driver</code> {@link Properties} entries for
	 *         connection details. Read-only and auto-commit for the
	 *         {@link Connection} are both set to <code>false</code>.
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown.
	 */
    protected Connection getUpgradeConnection() throws Exception {
        Connection retval = null;
        String url = properties.getProperty("database-url");

        Properties props = new Properties();
        props.setProperty("user", properties.getProperty("database-user"));
        props.setProperty("password", properties.getProperty("database-password"));

		LOGGER.info("Connecting to db " + properties.getProperty("database-name") + "...");
		LOGGER.info("url=" + url);

        Class.forName(properties.getProperty("database-driver"));
        retval = DriverManager.getConnection(url, props);
        retval.setReadOnly(false);
        retval.setAutoCommit(false);

		LOGGER.info("connected to database " + properties.getProperty("database-name"));
		LOGGER.info("");

        return retval;
    }

	/**
	 * Wrapper to do exception handling around closing database objects.
	 * 
	 * @param conn
	 *            {@link Connection} to close, if any
	 * @param stmt
	 *            {@link Statement} to close, if any
	 * @param res
	 *            {@link ResultSet} to close, if any
	 */
    protected void closeDbObjects(Connection conn, Statement stmt, ResultSet res) {
        try {
            if (res != null) {
                res.close();
            }
        } catch (Exception ex) {
        };
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception ex) {
        };
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception ex) {
        };
    }

	/**
	 * Convenience method to handle exeucting SQL statements and exception
	 * handling
	 * 
	 * @param conn
	 *            {@link Connection} to execute SQL against
	 * @param stmt
	 *            {@link Statement} to execute the SQL with
	 * @param sql
	 *            {@link String} of the SQL to execute
	 * @return <code>true</code> if the <code>sql</code> was executed
	 *         successfully, <code>false</code> otherwise.
	 */
    private boolean executeSql(Connection conn, Statement stmt, String sql) {
        boolean retval = false;

        try {
            if (isDDL(sql)) {
                stmt.execute(sql);
            } else {
                stmt.executeUpdate(sql);
            }

            retval = true;
        } catch (Exception ex) {
			LOGGER.error(ex);
        }

        return retval;
    }

	/**
	 * @param sql
	 *            {@link String} of the sql to check
	 * @return <code>true</code> if the provided <code>sql</code> does NOT start
	 *         with <code>INSERT</code>, <code>UPDATE</code>, or
	 *         <code>DELETE</code>; <code>false</code> otherwise
	 */
    private boolean isDDL(String sql) {
        String s = sql.toUpperCase();
        return (!s.toUpperCase().startsWith("UPDATE") && !s.startsWith("INSERT") && !s.startsWith("DELETE"));
    }

	/**
	 * Convenience wrapper to {@link FileUtils#forceDelete(File)} whether or not
	 * the file exists
	 * 
	 * @param f
	 *            {@link File} to delete
	 * @throws IOException
	 *             Any {@link IOException} encountered OTHER than
	 *             {@link FileNotFoundException}s will be rethrown
	 */
    private void deleteFile(File f) throws IOException {
        try {
            FileUtils.forceDelete(f);
        } catch (FileNotFoundException ex) {
        };
    }

	/**
	 * Perform pre-processing setup, such as cleaning out old logs and updating
	 * indices TODO note that updating indices is currently not running
	 * 
	 * @param conn
	 *            {@link Connection} to database
	 * @param stmt
	 *            {@link Statement} to execute SQL against
	 * @return <code>true</code> if all commands executed successfully,
	 *         <code>false</code> otherwise
	 */
    private boolean doInitialProcessing(Connection conn, Statement stmt) {
        boolean retval = false;
        ResultSet res = null;
        ResultSet res2 = null;
        PreparedStatement stmt2 = null;
        try {
            deleteFile(new File(properties.getProperty("output-log-file-name")));
            deleteFile(new File(properties.getProperty("processed-files-file-name")));

            logHeader1("pre-upgrade processing");
            logHeader2("dropping materialized view logs...");
            res = stmt.executeQuery("select LOG_OWNER || '.' || MASTER from SYS.user_mview_logs");

            List<String> logs = new ArrayList<String>();

            while (res.next()) {
                logs.add(res.getString(1));
            }

            for (String log : logs) {
                stmt.execute("drop materialized view log on " + log);
				LOGGER.info("dropped materialized view log on " + log);
            }

            res.close();

            logHeader2("ensuring combination of (SORT_CD, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ACTV_IND) unique on KRIM_TYP_ATTR_T...");

            StringBuilder sql = new StringBuilder(256);
            sql.append("select count(*), SORT_CD, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ACTV_IND ");
            sql.append("from KRIM_TYP_ATTR_T group by SORT_CD, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ACTV_IND ");
            sql.append("having count(*) > 1");
            res = stmt.executeQuery(sql.toString());

            while (res.next()) {
                if (stmt2 == null) {
                    stmt2 = conn.prepareStatement("select KIM_TYP_ATTR_ID from KRIM_TYP_ATTR_T where sort_cd = ? and KIM_TYP_ID = ? and  KIM_ATTR_DEFN_ID = ? and ACTV_IND = ?");
                }
                String sortCd = res.getString(1);
                stmt2.setString(1, sortCd);
                stmt2.setString(2, res.getString(2));
                stmt2.setString(3, res.getString(3));
                stmt2.setString(4, res.getString(4));

                res2 = stmt2.executeQuery();

                int indx = 0;
				// FIXME dead code; indx is NEVER > 0
                while (res2.next()) {
                    if (indx > 0) {
                        indx++;
                        if (sortCd.length() == 1) {
                            stmt.executeUpdate("update KRIM_TYP_ATTR_T set sort_cd = '" + (sortCd + indx) + "' where KIM_TYP_ATTR_ID = '" + res2.getString(1) + "'");
                        }
                    }
                }
            }

            retval = true;
        } catch (Exception ex) {
			LOGGER.error(ex);
        } finally {
            closeDbObjects(null, null, res);
            closeDbObjects(null, stmt2, res2);
        }

        return retval;
    }
    
	/**
	 * {@link Logger#info(Object)} the provided <code>message</code> encased in
	 * a block of '='s for emphasis
	 * 
	 * @param msg
	 */    
    public void logHeader1(String msg) {
		LOGGER.info(HEADER1.replace("?", msg));
    }
    
    
	/**
	 * {@link Logger#info(Object)} the provided <code>message</code> followed by
	 * a line of dashes for emphasis
	 * 
	 * @param msg
	 */    
    public void logHeader2(String msg) {
		LOGGER.info(msg);
		LOGGER.info(UNDERLINE);
    }

	/**
	 * 
	 * @return {@link PrintWriter} targeting the output log file specified by
	 *         the <code>output-log-file-name</code> {@link Properties} entry
	 * @throws IOException
	 *             Any {@link IOException}s encountered will be rethrown
	 */
	private PrintWriter getOutputLogWriter() throws IOException {
		return new PrintWriter(new FileWriter(properties.getProperty("output-log-file-name"), true));
	}

	/**
	 * @return {@link PrintWriter} targetting the processed files log file
	 *         specified by the <code>processed-files-file-name</code>
	 *         {@link Properties} entry
	 * @throws IOException
	 *             Any {@link IOException}s encountered will be rethrown
	 */
    private PrintWriter getProcessedFilesWriter() throws IOException {
        return new PrintWriter(new FileWriter(properties.getProperty("processed-files-file-name"), true));
    }

	/**
	 * Write the provided <code>txt</code> to the processed files log.
	 * 
	 * @param txt
	 * @see {@link #getProcessedFilesWriter()}
	 */
	public void writeProcessedFileInfo(String txt) {
        PrintWriter pw = null;
        try {
            pw = getProcessedFilesWriter();
            pw.println(getTimeString() + "--" + txt);
        } catch (Exception ex) {
			LOGGER.error(ex);
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex) {
            };
        }
    }

	/**
	 * Extracts the table name from a SQL statement using {@link String}
	 * manipulation based on the location of the sequence "<code> ON </code>"
	 * (including spaces)
	 * 
	 * @param line
	 *            {@link String} of a SQL statement to extract the table name
	 *            from
	 * @return {@link String} of the SQL table name if it is found,
	 *         <code>null</code> otherwise
	 */
    private String getIndexTableName(String line) {
        String retval = null;
        int pos = line.indexOf(" ON ");

        if (pos > -1) {
            pos = line.indexOf(".", pos);
            int pos2 = line.indexOf("(", pos);

            if ((pos > -1) && (pos2 > -1) && (pos2 > pos)) {
                retval = line.substring(pos + 1, pos2);
            }
        }

        return retval;
    }

	/**
	 * Extracts the name of the index column from a SQL statement using
	 * {@link String} manipulation based on the location of the sequence
	 * "<code> UNIQUE </code>" (including spaces)
	 * 
	 * @param line
	 *            {@link String} of the SQL statement to extract the index
	 *            column from
	 * @return {@link String} of the index column if found, <code>null</code>
	 *         otherwise
	 */
    private String getIndexName(String line) {
        String retval = null;
        StringTokenizer st = new StringTokenizer(line);
        if (st.countTokens() > 4) {
            int cnt = 2;
            if (line.contains(" UNIQUE ")) {
                cnt = 3;
            }

            for (int i = 0; i < cnt; ++i) {
                st.nextToken();
            }

            retval = st.nextToken().substring("KULOWNER.".length());
        }
        return retval;
    }

	/**
	 * Extracts the {@link List} of {@link String} column names from a SQL
	 * statement based on the first instance of a parens block
	 * 
	 * @param line
	 *            {@link String} of a SQL statement to extract the column names
	 *            from
	 * @return {@link List} of {@link String} column names extracted from the
	 *         SQL, if any; if no columns are found, will be a {@link List} of
	 *         size 0.
	 */
    private List<String> getIndexColumnNames(String line) {
        List<String> retval = new ArrayList<String>();

        int pos = line.indexOf("(");
        int pos2 = line.indexOf(")");

        if ((pos > -1) && (pos2 > -1) && (pos2 > pos)) {
            StringTokenizer st = new StringTokenizer(line.substring(pos + 1, pos2), ",");

            while (st.hasMoreTokens()) {
                retval.add(st.nextToken().trim());
            }
        }

        return retval;
    }

	/**
	 * From the {@link #upgradeRoot}
	 * <code>/post-upgrade/sql/kfs-indexes.sql</code> file, create any indices
	 * that are present in the SQL file but in the database that is being worked
	 * against TODO there's more going on here... come back after digging
	 * through submethods
	 * 
	 * @param conn
	 * @param stmt
	 */
    protected boolean createExistingIndexes(Connection conn, Statement stmt, File kfsIndexesSqlFile) {
        boolean success = true;
        LineNumberReader lnr = null;

        logHeader2("creating KFS indexes that existed prior to upgrade where required ");
        try {
			lnr = new LineNumberReader(new FileReader(kfsIndexesSqlFile));

            String line = null;

            while ((line = lnr.readLine()) != null) {

                if (StringUtils.isNotBlank(line) && line.startsWith("--")) {
                    // Skip lines starting with a comment
                    continue;
                }

                String tableName = getIndexTableName(line);
                String indexName = getIndexName(line);
                if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(indexName)) {
                    if (tableExists(conn, stmt, tableName)) {
                        boolean unique = line.contains(" UNIQUE ");
                        List<String> columnNames = getIndexColumnNames(line);

                        if (!indexExists(conn, stmt, tableName, columnNames)) {
                            if (indexNameExists(conn, stmt, tableName, indexName)) {
                                indexName = getNextTableIndexName(conn, stmt, tableName);
                            }

                            StringBuilder sql = new StringBuilder(256);

                            sql.append("CREATE ");

                            if (unique) {
                                sql.append("UNIQUE ");
                            }

                            sql.append("INDEX KULOWNER.");

                            sql.append(indexName);
                            sql.append(" ON KULOWNER.");
                            sql.append(tableName);
                            sql.append("(");

                            String comma = "";
                            for (String columnName : columnNames) {
                                sql.append(comma);
                                sql.append(columnName);
                                comma = ",";
                            }

                            sql.append(")");

                            try {
                                stmt.execute(sql.toString());
                            } catch (SQLException ex) {
                                success = false;
								LOGGER.error("failed to create index: " + sql.toString(), ex);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
			success = false;
			LOGGER.error(ex);
        } finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception ex) {
            };
        }
		postUpgradeFilesProcessed.add(kfsIndexesSqlFile);

        return success;
    }

	/**
	 * @param conn
	 *            {@link Connection} to the target database
	 * @param stmt
	 *            {@link Statement} to use to execute SQL statements
	 * @param tableName
	 * @return <code>true</code> if the table <code>tableName</code> exists in
	 *         the database connected to by <code>conn</code>;
	 *         <code>false</code>otherwise
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 */
    private boolean tableExists(Connection conn, Statement stmt, String tableName) throws Exception {
        boolean retval = false;
        ResultSet res = null;

        try {
            res = stmt.executeQuery("select count(*) from user_tables where table_name = '" + tableName + "'");

            if (res.next()) {
                retval = (res.getInt(1) > 0);
            }
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;

    }

	/**
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} to execute SQL against
	 * @param tableName
	 *            {@link String} of the table name to check for an existing
	 *            index
	 * @param columnNames
	 *            {@link List} of {@link String} column names to check for the
	 *            existence of an index against
	 * @return <code>true</code> if there exists an index on the table
	 *         <code>tableName</code> on the same columns as
	 *         <code>columnNames</code>; <code>false</code> otherwise
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 */
	/*
	 * TODO Investigate: this doesn't check on the NAME of the index... does
	 * that matter?
	 */
    private boolean indexExists(Connection conn, Statement stmt, String tableName, List<String> columnNames) throws Exception {
        boolean retval = false;
        ResultSet res = null;

        try {
            StringBuilder sql = new StringBuilder(256);

            sql.append("select index_name, column_name, column_position ");
            sql.append("from all_ind_columns ");
            sql.append("where index_owner = 'KULOWNER' ");
            sql.append("and table_owner = 'KULOWNER' ");
            sql.append("and table_name = '");
            sql.append(tableName);
            sql.append("' order by index_name, column_position");

            Map<String, List<String>> map = new HashMap<String, List<String>>();

            res = stmt.executeQuery(sql.toString());
            while (res.next()) {
                String indexName = res.getString(1);

                List<String> columns = map.get(indexName);

                if (columns == null) {
                    map.put(indexName, columns = new ArrayList<String>());
                }

                columns.add(res.getString(2).trim());
            }

			/*
			 * for each index name, if the list of columns in that index is the
			 * same size of the input columnNames, AND the contents are the same
			 */
            for (List<String> columns : map.values()) {
                if (columns.size() == columnNames.size()) {
                    boolean foundit = true;
                    for (String column : columns) {
                        if (!columnNames.contains(column)) {
                            foundit = false;
                            break;
                        }
                    }

                    if (foundit) {
                        retval = true;
                        break;
                    }
                }
            }
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;
    }

	/**
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} to use to execute SQL
	 * @param tableName
	 *            {@link String} of the name of the table to query against
	 * @param indexName
	 *            {@link String} of the name of the index to query for the
	 *            existence of
	 * @return <code>true</code> if there is an index named
	 *         <code>indexName</code> on the table <code>tableName</code>;
	 *         <code>false</code> otherwise
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 */
    private boolean indexNameExists(Connection conn, Statement stmt, String tableName, String indexName) throws Exception {
        boolean retval = false;
        ResultSet res = null;

        try {
            res = stmt.executeQuery("select count(*) from user_indexes where table_owner = 'KULOWNER' and table_name = '" + tableName + "' and index_name = '" + indexName + "'");
            if (res.next()) {
                retval = (res.getInt(1) > 0);
            }
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;
    }

	/**
	 * Queries for indexes against the provided <code>tableName</code> and looks
	 * for indices named like <code>*_TI#</code>, finds the one with the highest
	 * trailing number <code>n</code>, and returns a {@link String} of the
	 * format <code>[tableName]I(n+1)</code>
	 * 
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} to execute SQL with
	 * @param tableName
	 *            {@link String} of the table name to check on for indices
	 * @return {@link String} of the format <code>[tableName]I(n+1)</code>
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 */
	/*
	 * NOTE: Extremely tightly coupled to the kfs-indexes.sql resource file...
	 * how often does that change? If ever?
	 */
    private String getNextTableIndexName(Connection conn, Statement stmt, String tableName) throws Exception {
        String retval = null;

        int maxIndex = -1;
        ResultSet res = null;

        try {
            res = stmt.executeQuery("select index_name from user_indexes where table_owner = 'KULOWNER' and table_name = '" + tableName + "'");
            while (res.next()) {
                String indexName = res.getString(1);
                int pos = indexName.lastIndexOf("_");

                if (pos > -1) {
                    if (indexName.substring(pos + 1).startsWith("TI")) {
                        try {
                            int i = Integer.parseInt(indexName.substring(pos + 3));
                            if (i > maxIndex) {
                                maxIndex = i;

                            }
                        } catch (NumberFormatException ex) {
                        };
                    }
                }
            }

            retval = (tableName + "I" + (maxIndex + 1));
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;
    }

	/**
	 * <strong>Note:</strong> assumes that <code>line</code> is
	 * <code>CREATE PUBLIC SYNONYM [X] FOR [Y]</code> SQL statement
	 * <p>
	 * 
	 * @param line
	 *            {@link String} of a SQL create synonym statement
	 * @return {@link String} of the name of the synonym being created in
	 *         <code>line</code>
	 */
    private String getSynonymName(String line) {
        String retval = null;
        StringTokenizer st = new StringTokenizer(line);

        st.nextToken();
        st.nextToken();
        st.nextToken();
        String token = st.nextToken();

        int pos = token.lastIndexOf(";");

        if (pos > -1) {
            retval = token.substring(0, pos).trim();
        } else {
            retval = token.trim();
        }

        return retval;
    }

	/**
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} to use to execute SQL statements
	 * @param synonymName
	 *            {@link String} of the synonym to check for the existence of
	 * @return <code>true</code> if the synonym exists, <code>false</code>
	 *         otherwise
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 */
    private boolean synonymExists(Connection conn, Statement stmt, String synonymName) throws Exception {
        boolean retval = false;
        ResultSet res = null;

        try {
            res = stmt.executeQuery("select count(*) from all_synonyms where owner = 'PUBLIC' and synonym_name = '" + synonymName + "'");
            if (res.next()) {
                retval = (res.getInt(1) > 0);
            }
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;
    }

	/**
	 * Create the public synonyms specified in {@link #upgradeRoot}
	 * <code>/post-upgrade/sql/kfs-public-synonyms.sql</code> that do not
	 * already exist.
	 * 
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} to use to execute SQL statements
	 */
    private void createPublicSynonyms(Connection conn, Statement stmt) {
        LineNumberReader lnr = null;

        logHeader2("creating KFS public synonyms that existed prior to upgrade where required ");

		File kfsPublicSynonymsSqlFile = new File(postUpgradeDirectory + File.separator + KFS_PUBLIC_SYNONYMS_SQL_PATH);
        try {
			lnr = new LineNumberReader(new FileReader(kfsPublicSynonymsSqlFile));

            String line = null;

            while ((line = lnr.readLine()) != null) {
                String synonymName = getSynonymName(line);

                if (!synonymExists(conn, stmt, synonymName)) {
                    try {
						// if there is a trailing semicolon, remove it
						int pos = line.lastIndexOf(';');
						if (pos == line.length() - 1) {
							line = line.substring(0, line.length() - 1);
						}
                        stmt.execute(line);
                    } catch (SQLException ex) {
						LOGGER.error("failed to create public synonym: " + line, ex);
                    }
                }
            }
        } catch (Exception ex) {
			LOGGER.error(ex);
        } finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception ex) {
				LOGGER.error(ex);
            };
        }
		postUpgradeFilesProcessed.add(kfsPublicSynonymsSqlFile);
    }

	/**
	 * Gets the indexes from a {@link DatabaseMetaData} for a given
	 * <code>table</code> and creates and returns a {@link Set} of
	 * {@link String} SQL UPDATE statements that will create those indexes on a
	 * database.
	 * 
	 * @param dmd
	 *            {@link DatabaseMetaData} to get index information from
	 * @param table
	 *            {@link String} of the specific table to get index information
	 *            for
	 * @return {@link Set} of {@link String} SQL UPDATE statements that will
	 *         create those indexes on a database.
	 */
    private Set<String> loadForeignKeyIndexInformation(DatabaseMetaData dmd, String table) {
        Set<String> retval = new HashSet<String>();

        ResultSet res = null;
        try {
            Map<String, ForeignKeyReference> fkeys = new HashMap<String, ForeignKeyReference>();
            Map<String, TableIndexInfo> tindexes = new HashMap<String, TableIndexInfo>();
            res = dmd.getImportedKeys(null, getSchema(), table);
            boolean foundfk = false;

            while (res.next()) {
                foundfk = true;
				// FKCOLUMN_NAME String => foreign key column name
				String fcname = res.getString(8);
				/*
				 * KEY_SEQ short => sequence number within a foreign key( a
				 * value of 1 represents the first column of the foreign key, a
				 * value of 2 would represent the second column within the
				 * foreign key)
				 */
                int seq = res.getInt(9);
				// 12 - FK_NAME String => foreign key name (may be null)
                String fkname = res.getString(12);

                ForeignKeyReference fkref = fkeys.get(fkname);

                if (fkref == null) {
                    fkeys.put(fkname, fkref = new ForeignKeyReference(getSchema(), table, fkname, INDEX_NAME_TEMPLATE));
                }

                ColumnInfo cinfo = new ColumnInfo(fcname, seq);

                cinfo.setNumeric(isNumericColumn(dmd, getSchema(), table, fcname));

                fkref.addColumn(cinfo);
            }

            res.close();

            if (foundfk) {
                tindexes.put(table, loadTableIndexInfo(dmd, table));
            }

            List<ForeignKeyReference> l = new ArrayList<ForeignKeyReference>(fkeys.values());

            Collections.sort(l);

            Iterator<ForeignKeyReference> it = l.iterator();

            while (it.hasNext()) {
                ForeignKeyReference fkref = it.next();
                if (hasIndex(tindexes.get(fkref.getTableName()).getIndexes(), fkref)) {
                    it.remove();
                } else {
                    String s = fkref.getCreateIndexString(tindexes.get(fkref.getTableName()));
                    if (StringUtils.isNotBlank(fkref.getIndexName())) {
                        retval.add(s);
                    }
                }
            }
        } catch (Exception ex) {
			LOGGER.error(ex);
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;
    }

	/**
	 * Constructs a tables {@link TableIndexInfo} from the
	 * {@link DatabaseMetaData} for a specific table name <code>tname</code>.
	 * 
	 * @param dmd
	 *            {@link DatabaseMetaData} describing the database
	 * @param tname
	 *            {@link String} of the table name to construct the
	 *            {@link TableIndexInfo} for
	 * @return {@link TableIndexInfo} for the table with the given table name
	 *         <code>tname</code>
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 */
    private TableIndexInfo loadTableIndexInfo(DatabaseMetaData dmd, String tname) throws Exception {
        TableIndexInfo retval = new TableIndexInfo(tname);
        ResultSet res = null;

        try {
            Map<String, IndexInfo> imap = new HashMap<String, IndexInfo>();

            res = dmd.getIndexInfo(null, getSchema(), tname, false, true);

            while (res.next()) {
				/*
				 * INDEX_NAME String => index name; null when TYPE is
				 * tableIndexStatistic
				 */
                String iname = res.getString(6);

                if (iname != null) {
					/*
					 * COLUMN_NAME String => column name; null when TYPE is
					 * tableIndexStatistic
					 */
                    String cname = res.getString(9);

                    IndexInfo i = imap.get(iname);

                    if (i == null) {
                        imap.put(iname, i = new IndexInfo(iname));
                    }

                    i.addColumn(cname);
                }
            }

            retval.getIndexes().addAll(imap.values());

            for (IndexInfo i : retval.getIndexes()) {
                String indexName = i.getIndexName();

                int indx = 1;
                for (int j = indexName.length() - 1; j >= 0; --j) {
                    if (!Character.isDigit(indexName.charAt(j))) {
                        try {
                            indx = Integer.parseInt(indexName.substring(j + 1));
                        } catch (NumberFormatException ex) {
                        };

                        break;
                    }
                }

                if (retval.getMaxIndexSuffix() < indx) {
                    retval.setMaxIndexSuffix(indx);
                }
            }
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;
    }

	/**
	 * @param indexes
	 *            {@link List} of {@link IndexInfo} to check for the existence
	 *            of <code>fkref</code> in
	 * @param fkref
	 *            {@link ForeignKeyReference} to check for the existence of in
	 *            <code>indexes</code>
	 * @return <code>true</code> if <code>indexes</code> contains
	 *         <code>fkref</code>, <code>false</code> otherwise
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 */
    private boolean hasIndex(List<IndexInfo> indexes, ForeignKeyReference fkref) throws Exception {
        boolean retval = false;

        for (IndexInfo i : indexes) {
            if (fkref.getColumns().size() == i.getIndexColumns().size()) {
                boolean foundit = true;
                for (ColumnInfo cinfo : fkref.getColumns()) {
                    if (!i.getIndexColumns().contains(cinfo.getColumnName())) {
                        foundit = false;
                    }
                }

                if (foundit) {
                    retval = true;
                    break;
                }
            }
        }

        return retval;
    }

	/**
	 * @return {@link String} of this {@link App}'s <code>database-schema</code>
	 *         {@link Properties} entry
	 */
    private String getSchema() {
        return properties.getProperty("database-schema");
    }

	/**
	 * @param dmd
	 *            {@link DatabaseMetaData} describing the database to check
	 * @param schema
	 *            {@link String} of the schema name to check
	 * @param tname
	 *            {@link String} of the table name to check
	 * @param cname
	 *            {@link String} of the column name to check
	 * @return <code>true</code> if the column <code>cname</code> in the table
	 *         <code>tname</code> in the schema <code>schema</code> in the
	 *         database described by <code>dmd</code> is a Java numeric type;
	 *         <code>false</code> otherwise
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 * @see {@link #isNumericJavaType(int)}
	 */
    private boolean isNumericColumn(DatabaseMetaData dmd, String schema, String tname, String cname) throws Exception {
        boolean retval = false;

        ResultSet res = null;

        try {
            res = dmd.getColumns(null, schema, tname, cname);

            if (res.next()) {
                retval = isNumericJavaType(res.getInt(5));

            }
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;
    }

	/**
	 * @param type
	 *            <code>int</code> of a {@link Types} value
	 * @return <code>true</code> if <code>type</code> is one of the following:
	 *         <ul>
	 *         <li>{@link Types#BIGINT}</li>
	 *         <li>{@link Types#BINARY}</li>
	 *         <li>{@link Types#DECIMAL}</li>
	 *         <li>{@link Types#DOUBLE}</li>
	 *         <li>{@link Types#FLOAT}</li>
	 *         <li>{@link Types#INTEGER}</li>
	 *         <li>{@link Types#NUMERIC}</li>
	 *         <li>{@link Types#REAL}</li>
	 *         <li>{@link Types#SMALLINT}</li>
	 *         <li>{@link Types#TINYINT}</li>
	 *         </ul>
	 *         <p>
	 *         , <code>false</code> otherwise.
	 */
    private boolean isNumericJavaType(int type) {
        return ((type == java.sql.Types.BIGINT)
            || (type == java.sql.Types.BINARY)
            || (type == java.sql.Types.DECIMAL)
            || (type == java.sql.Types.DOUBLE)
            || (type == java.sql.Types.FLOAT)
            || (type == java.sql.Types.INTEGER)
            || (type == java.sql.Types.NUMERIC)
            || (type == java.sql.Types.REAL)
            || (type == java.sql.Types.SMALLINT)
            || (type == java.sql.Types.TINYINT));
    }

	/**
	 * Use the {@link DatabaseMetaData} from the given {@link Connection} to
	 * generate the foreign key index information for each table
	 * 
	 * @param conn
	 *            {@link Connection} to the database
	 * @param stmt
	 *            {@link Statement} to use to execute SQL
	 * @see {@link #loadForeignKeyIndexInformation(DatabaseMetaData, String)}
	 */
    private void createForeignKeyIndexes(Connection conn, Statement stmt) {
        logHeader2("creating indexes on foreign keys where required...");
        ResultSet res = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();
            res = dmd.getTables(null, getSchema(), null, new String[]{"TABLE"});

            while (res.next()) {
				// TABLE_NAME String => table name
                String tname = res.getString(3);
				/*
				 * for each table name, load foreign key index information to
				 * get SQL updates to execute, then execute each SQL update
				 * statement
				 */

                Set<String> sqllist = loadForeignKeyIndexInformation(dmd, tname);

                if ((sqllist != null) && !sqllist.isEmpty()) {
					LOGGER.info("creating required foreign key indexes on table " + tname + "...");
                    int cnt = 0;
                    for (String sql : sqllist) {
                        try {
                            stmt.executeQuery(sql);
                            cnt++;
                        } catch (Exception ex) {
							LOGGER.error("create index failed: " + sql, ex);
                        }
                    }

					LOGGER.info("    " + cnt + " indexes created");
                }
            }
        } catch (Exception ex) {
			LOGGER.info(ex);
        } finally {
            closeDbObjects(null, null, res);
        }
    }

	/**
	 * Execute the following prepared statement:
	 * <code>insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'displayType', 'document')</code>
	 * with the parameter specified by values returned by
	 * <code>select distinct doc_hdr_id from krew_doc_hdr_ext_t order by  1</code>
	 * . All updates are done in a single transaction; any {@link Exception}s
	 * encountered will cause the transaction to rollback.
	 * 
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} that is immediately blown away and should be
	 *            removed as a parameter
	 */
    private void createDocumentSearchEntries(Connection conn, Statement stmt) {
		logHeader2("Creating document search entries.");
        PreparedStatement pstmt = null;
        ResultSet res = null;
        try {
            conn.setAutoCommit(false);
            stmt = conn.createStatement();

            pstmt = conn.prepareStatement("insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'displayType', 'document')");

            int i = 1;
            res = stmt.executeQuery("select distinct doc_hdr_id from krew_doc_hdr_ext_t order by  1");

            while (res.next()) {
                pstmt.setString(1, res.getString(1));
				pstmt.addBatch();

                if ((i % 10000) == 0) {
					pstmt.executeBatch();
					LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted");
                }

                i++;
            }

			// catch any straggler statements
			pstmt.executeBatch();
			LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted TOTAL");

            conn.commit();
        } catch (Exception ex) {
			LOGGER.error(ex);

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex2) {
					LOGGER.error(ex);
                };
            }
        } finally {
            try {
                closeDbObjects(null, pstmt, res);
            } catch (Exception ex) {
				LOGGER.error(ex);
            };
        }
    }

	/**
	 * Execute the following prepared statement:
	 * <code>insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)</code>
	 * with the parameters specified by values returned by
	 * <code>SELECT P.FDOC_NBR, S.PO_STAT_DESC FROM PUR_PO_T P, DEPR_PUR_PO_STAT_T S WHERE P.DEPR_PO_STAT_CD = S.PO_STAT_CD</code>
	 * . All updates are done in a single transaction; any {@link Exception}s
	 * encountered will cause the transaction to rollback.
	 *
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} that is immediately blown away and should be
	 *            removed as a parameter
	 */
	private void addPODocStatus(Connection conn, Statement stmt) {
		logHeader2("Adding PO document status entries.");
		PreparedStatement insertStmt = null;
		ResultSet legacyRes = null;
		try {
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			insertStmt = conn.prepareStatement("insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)");
			int i = 1;
			//first, get the list of PO document numbers with the matching textual application document status
			legacyRes = stmt.executeQuery("SELECT P.FDOC_NBR, S.PO_STAT_DESC FROM PUR_PO_T P, DEPR_PUR_PO_STAT_T S WHERE P.DEPR_PO_STAT_CD = S.PO_STAT_CD");

			//then loop through the list of PO document numbers, populating and executing the insert statements
			while (legacyRes.next()) {
				String docNbr = legacyRes.getString(1);
				String desc = legacyRes.getString(2);
				insertStmt.setString(1, docNbr);
				insertStmt.setString(2, desc.replace("&", "and"));
				insertStmt.addBatch();
				if ((i % 10000) == 0) {
					insertStmt.executeBatch();
					LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted");
				}
				i++;
			}
			// catch any straggler statements
			insertStmt.executeBatch();
			LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted TOTAL");
			conn.commit();
		} catch (Exception ex) {
			LOGGER.error(ex);

			if (conn != null) {
				try {
					conn.rollback();
				} catch (Exception ex2) {
					LOGGER.error(ex);
				};
			}
		} finally {
			try {
				closeDbObjects(null, insertStmt, legacyRes);
			} catch (Exception ex) {
				LOGGER.error(ex);
			};
		}
	}

	/**
	 * Execute the following prepared statement:
	 * <code>insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)</code>
	 * with the parameters specified by values returned by
	 * <code>SELECT P.FDOC_NBR, S.CRDT_MEMO_STAT_DESC FROM AP_CRDT_MEMO_T P, DEPR_AP_CRDT_MEMO_STAT_T S WHERE P.DEPR_CRDT_MEMO_STAT_CD = S.CRDT_MEMO_STAT_CD</code>
	 * . All updates are done in a single transaction; any {@link Exception}s
	 * encountered will cause the transaction to rollback.
	 *
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} that is immediately blown away and should be
	 *            removed as a parameter
	 */
	private void addVendorCreditMemoDocStatus(Connection conn, Statement stmt) {
		logHeader2("Adding Vendor Credit Memo document status entries.");
		PreparedStatement insertStmt = null;
		ResultSet legacyRes = null;
		try {
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			insertStmt = conn.prepareStatement("insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)");
			int i = 1;
			//first, get the list of PO document numbers with the matching textual application document status
			legacyRes = stmt.executeQuery("SELECT P.FDOC_NBR, S.CRDT_MEMO_STAT_DESC FROM AP_CRDT_MEMO_T P, DEPR_AP_CRDT_MEMO_STAT_T S WHERE P.DEPR_CRDT_MEMO_STAT_CD = S.CRDT_MEMO_STAT_CD");

			//then loop through the list of PO document numbers, populating and executing the insert statements
			while (legacyRes.next()) {
				String docNbr = legacyRes.getString(1);
				String desc = legacyRes.getString(2);
				insertStmt.setString(1, docNbr);
				insertStmt.setString(2, desc.replace("&", "and"));
				insertStmt.addBatch();
				if ((i % 10000) == 0) {
					insertStmt.executeBatch();
					LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted");
				}
				i++;
			}
			// catch any straggler statements
			insertStmt.executeBatch();
			LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted TOTAL");
			conn.commit();
		} catch (Exception ex) {
			LOGGER.error(ex);

			if (conn != null) {
				try {
					conn.rollback();
				} catch (Exception ex2) {
					LOGGER.error(ex);
				};
			}
		} finally {
			try {
				closeDbObjects(null, insertStmt, legacyRes);
			} catch (Exception ex) {
				LOGGER.error(ex);
			};
		}
	}

	/**
	 * Execute the following prepared statement:
	 * <code>insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)</code>
	 * with the parameters specified by values returned by
	 * <code>SELECT P.FDOC_NBR, S.PMT_RQST_STAT_DESC FROM AP_PMT_RQST_T P, DEPR_AP_PMT_RQST_STAT_T S WHERE P.DEPR_PMT_RQST_STAT_CD = S.PMT_RQST_STAT_CD</code>
	 * . All updates are done in a single transaction; any {@link Exception}s
	 * encountered will cause the transaction to rollback.
	 *
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} that is immediately blown away and should be
	 *            removed as a parameter
	 */
	private void addPREQDocStatus(Connection conn, Statement stmt) {
		logHeader2("Adding Payment Request document status entries.");
		PreparedStatement insertStmt = null;
		ResultSet legacyRes = null;
		try {
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			insertStmt = conn.prepareStatement("insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)");
			int i = 1;
			//first, get the list of PO document numbers with the matching textual application document status
			legacyRes = stmt.executeQuery("SELECT P.FDOC_NBR, S.PMT_RQST_STAT_DESC FROM AP_PMT_RQST_T P, DEPR_AP_PMT_RQST_STAT_T S WHERE P.DEPR_PMT_RQST_STAT_CD = S.PMT_RQST_STAT_CD");

			//then loop through the list of PO document numbers, populating and executing the insert statements
			while (legacyRes.next()) {
				String docNbr = legacyRes.getString(1);
				String desc = legacyRes.getString(2);
				insertStmt.setString(1, docNbr);
				insertStmt.setString(2, desc.replace("&", "and"));
				insertStmt.addBatch();
				if ((i % 10000) == 0) {
					insertStmt.executeBatch();
					LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted");
				}
				i++;
			}
			// catch any straggler statements
			insertStmt.executeBatch();
			LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted TOTAL");
			conn.commit();
		} catch (Exception ex) {
			LOGGER.error(ex);

			if (conn != null) {
				try {
					conn.rollback();
				} catch (Exception ex2) {
					LOGGER.error(ex);
				};
			}
		} finally {
			try {
				closeDbObjects(null, insertStmt, legacyRes);
			} catch (Exception ex) {
				LOGGER.error(ex);
			};
		}
	}

	/**
	 * Execute the following prepared statement:
	 * <code>insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)</code>
	 * with the parameters specified by values returned by
	 * <code>SELECT P.FDOC_NBR, S.RCVNG_LN_STAT_DESC FROM PUR_RCVNG_LN_T P, DEPR_PUR_RCVNG_LN_STAT_T S WHERE P.DEPR_RCVNG_LN_STAT_CD = S.RCVNG_LN_STAT_CD</code>
	 * . All updates are done in a single transaction; any {@link Exception}s
	 * encountered will cause the transaction to rollback.
	 *
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} that is immediately blown away and should be
	 *            removed as a parameter
	 */
	private void addLineItemReceivingDocStatus(Connection conn, Statement stmt) {
		logHeader2("Adding Purchasing Line Item Receiving document status entries.");
		PreparedStatement insertStmt = null;
		ResultSet legacyRes = null;
		try {
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			insertStmt = conn.prepareStatement("insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)");
			int i = 1;
			//first, get the list of PO document numbers with the matching textual application document status
			legacyRes = stmt.executeQuery("SELECT P.FDOC_NBR, S.RCVNG_LN_STAT_DESC FROM PUR_RCVNG_LN_T P, DEPR_PUR_RCVNG_LN_STAT_T S WHERE P.DEPR_RCVNG_LN_STAT_CD = S.RCVNG_LN_STAT_CD");

			//then loop through the list of PO document numbers, populating and executing the insert statements
			while (legacyRes.next()) {
				String docNbr = legacyRes.getString(1);
				String desc = legacyRes.getString(2);
				insertStmt.setString(1, docNbr);
				insertStmt.setString(2, desc.replace("&", "and"));
				insertStmt.addBatch();
				if ((i % 10000) == 0) {
					insertStmt.executeBatch();
					LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted");
				}
				i++;
			}
			// catch any straggler statements
			insertStmt.executeBatch();
			LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted TOTAL");
			conn.commit();
		} catch (Exception ex) {
			LOGGER.error(ex);

			if (conn != null) {
				try {
					conn.rollback();
				} catch (Exception ex2) {
					LOGGER.error(ex);
				};
			}
		} finally {
			try {
				closeDbObjects(null, insertStmt, legacyRes);
			} catch (Exception ex) {
				LOGGER.error(ex);
			};
		}
	}

	/**
	 * Execute the following prepared statement:
	 * <code>insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)</code>
	 * with the parameters specified by values returned by
	 * <code>SELECT P.FDOC_NBR, S.REQS_STAT_DESC FROM PUR_REQS_T P, DEPR_PUR_REQS_STAT_T S WHERE P.DEPR_REQS_STAT_CD = S.REQS_STAT_CD</code>
	 * . All updates are done in a single transaction; any {@link Exception}s
	 * encountered will cause the transaction to rollback.
	 *
	 * @param conn
	 *            {@link Connection} to a database
	 * @param stmt
	 *            {@link Statement} that is immediately blown away and should be
	 *            removed as a parameter
	 */
	private void addPurchaseRequisitionDocStatus(Connection conn, Statement stmt) {
		logHeader2("Adding Purchase Requistion document status entries.");
		PreparedStatement insertStmt = null;
		ResultSet legacyRes = null;
		try {
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			insertStmt = conn.prepareStatement("insert into krew_doc_hdr_ext_t (doc_hdr_ext_id, doc_hdr_id, key_cd, val) values (to_char(KREW_SRCH_ATTR_S.nextval), ?, 'applicationDocumentStatus', ?)");
			int i = 1;
			//first, get the list of PO document numbers with the matching textual application document status
			legacyRes = stmt.executeQuery("SELECT P.FDOC_NBR, S.REQS_STAT_DESC FROM PUR_REQS_T P, DEPR_PUR_REQS_STAT_T S WHERE P.DEPR_REQS_STAT_CD = S.REQS_STAT_CD");

			//then loop through the list of PO document numbers, populating and executing the insert statements
			while (legacyRes.next()) {
				String docNbr = legacyRes.getString(1);
				String desc = legacyRes.getString(2);
				insertStmt.setString(1, docNbr);
				insertStmt.setString(2, desc.replace("&", "and"));
				insertStmt.addBatch();
				if ((i % 10000) == 0) {
					insertStmt.executeBatch();
					LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted");
				}
				i++;
			}
			// catch any straggler statements
			insertStmt.executeBatch();
			LOGGER.info(i + " krew_doc_hdr_ext_t entries inserted TOTAL");
			conn.commit();
		} catch (Exception ex) {
			LOGGER.error(ex);

			if (conn != null) {
				try {
					conn.rollback();
				} catch (Exception ex2) {
					LOGGER.error(ex);
				};
			}
		} finally {
			try {
				closeDbObjects(null, insertStmt, legacyRes);
			} catch (Exception ex) {
				LOGGER.error(ex);
			};
		}
	}

	/**
	 * Execute the sql file {@link #upgradeRoot}
	 * <code>/post-upgrade/sql/misc.sql</code> against the database
	 * 
	 * @param conn
	 *            {@link Connection} to the database
	 * @param stmt
	 *            {@link Statement} to use to execute SQL
	 */
    private void runMiscSql(Connection conn, Statement stmt) {
        LineNumberReader lnr = null;

		logHeader2("Executing miscellaneous post-upgrade sql");
		File miscSqlFile = new File(postUpgradeDirectory + File.separator + MISC_SQL_PATH);
        try {
			lnr = new LineNumberReader(new FileReader(miscSqlFile));

            String sql = null;

            while ((sql = lnr.readLine()) != null) {
                if (StringUtils.isNotBlank(sql)) {
                    try {
                        if (sql.trim().endsWith(";")) {
                            int pos = sql.lastIndexOf(";");
                            sql = sql.substring(0, pos);
                        }
                    
                        if (isDDL(sql)) {
                            stmt.execute(sql);
                        } else {
                            stmt.executeUpdate(sql);
                        }
						LOGGER.info(sql);
                    } catch (SQLException ex) {
						LOGGER.error("sql execution failed: " + sql, ex);
                    }
                }
            }
        } catch (Exception ex) {
			LOGGER.error(ex);
        } finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception ex) {
				LOGGER.error(ex);
            };
        }
		postUpgradeFilesProcessed.add(miscSqlFile);
    }

	/**
	 * Update the purchasing statuses from the KFS3 tables to the KFS6 tables.
	 * All updates are done on a single transaction; if any {@link Exception}s
	 * are encountered, the transaction will be rolled back.
	 * 
	 * @param upgradeConn
	 *            {@link Connection} to the upgrade database
	 */
	/*
	 * TODO: Need a DB expert to explain what's going on here so can expand
	 * comment. All of this SQL should really be externalized its own script (or
	 * several scripts; there are a lot of subpieces here) which is documented
	 * on its own
	 */
    private void updatePurchasingStatuses(Connection upgradeConn) {
		logHeader2("Updating purchasing statuses.");
        Statement legacyStmt = null;
        ResultSet legacyRes = null;
        PreparedStatement upgradeStmt1 = null;
        PreparedStatement upgradeStmt2 = null;
        try {
            // load status names from legacy status tables
			legacyStmt = upgradeConn.createStatement();

			legacyRes = legacyStmt
					.executeQuery("select CRDT_MEMO_STAT_CD, CRDT_MEMO_STAT_DESC from DEPR_AP_CRDT_MEMO_STAT_T");

            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from AP_CRDT_MEMO_T where DEPR_CRDT_MEMO_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from AP_CRDT_MEMO_T where DEPR_CRDT_MEMO_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

				LOGGER.info("updating credit memo app_doc_stat[" + desc + "] in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

				LOGGER.info("updating credit memo app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            closeDbObjects(null, upgradeStmt1, legacyRes);
            closeDbObjects(null, upgradeStmt2, null);

			// Add Vendor Credit Memo Document status entries
			addVendorCreditMemoDocStatus(upgradeConn, legacyStmt);

			legacyRes = legacyStmt
					.executeQuery("select PMT_RQST_STAT_CD, PMT_RQST_STAT_DESC from DEPR_AP_PMT_RQST_STAT_T");
            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from AP_PMT_RQST_T where DEPR_PMT_RQST_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from AP_PMT_RQST_T where DEPR_PMT_RQST_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

				LOGGER.info("updating payment request app_doc_stat[" + desc + "]  in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

				LOGGER.info("updating payment request app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            closeDbObjects(null, upgradeStmt1, legacyRes);
            closeDbObjects(null, upgradeStmt2, null);

			// Add Payment Request document status entries
			addPREQDocStatus(upgradeConn, legacyStmt);

			legacyRes = legacyStmt.executeQuery("select PO_STAT_CD, PO_STAT_DESC from DEPR_PUR_PO_STAT_T");
            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from PUR_PO_T where DEPR_PO_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from PUR_PO_T where DEPR_PO_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

				LOGGER.info("updating purchase order app_doc_stat[" + desc + "]  in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

				LOGGER.info("updating purchase order app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            closeDbObjects(null, upgradeStmt1, legacyRes);
            closeDbObjects(null, upgradeStmt2, null);

			// Add Purchase Order document status entries
			addPODocStatus(upgradeConn, legacyStmt);

			legacyRes = legacyStmt
					.executeQuery("select RCVNG_LN_STAT_CD, RCVNG_LN_STAT_DESC from DEPR_PUR_RCVNG_LN_STAT_T");
            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from PUR_RCVNG_LN_T where DEPR_RCVNG_LN_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from PUR_RCVNG_LN_T where DEPR_RCVNG_LN_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

				LOGGER.info("updating purchase receiving line app_doc_stat[" + desc + "]  in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

				LOGGER.info("updating purchase receiving line app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            closeDbObjects(null, upgradeStmt1, legacyRes);
            closeDbObjects(null, upgradeStmt2, null);

			// Add Purchasing Line Item Receiving document status entries
			addLineItemReceivingDocStatus(upgradeConn, legacyStmt);

			legacyRes = legacyStmt.executeQuery("select REQS_STAT_CD, REQS_STAT_DESC from DEPR_PUR_REQS_STAT_T");
            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from PUR_REQS_T where DEPR_REQS_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from PUR_REQS_T where DEPR_REQS_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

				LOGGER.info("updating requisition app_doc_stat[" + desc + "]  in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

				LOGGER.info("updating requisition app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            //  Add Purchase Requistion document status entries
			addPurchaseRequisitionDocStatus(upgradeConn, legacyStmt);

            upgradeConn.commit();
        } catch (Exception ex) {
			LOGGER.error(ex);
            try {
                upgradeConn.rollback();
            } catch (Exception ex2) {
            };
        } finally {
			closeDbObjects(null, legacyStmt, legacyRes);
            closeDbObjects(null, upgradeStmt1, null);
            closeDbObjects(null, upgradeStmt2, null);
        }
    }

	/**
	 * Checks if the combination of <code>NM</code> and <code>NMSPC_CD</code> is
	 * unique on <code>KRIM_PERM_T</code> and <code>KRIM_RSP_T</code>. If there
	 * are duplicates, <code>PERM_ID</code> will be appended to them. If there
	 * are STILL duplicates, then <code>RSP_ID</code> will be appended to them.
	 * Any duplicates found that need to be updated are then written to the
	 * database.
	 * 
	 * @param conn
	 *            {@link Connection} to the database
	 * @param stmt
	 *            {@link Statement} to use to execute SQL against
	 * @return <code>true</code> if all SQL updates executed successfully;
	 *         <code>false</code> otherwise.
	 */
    private boolean ensureNmNmspccdUnique(Connection conn, Statement stmt) {
        boolean retval = false;
        ResultSet res = null;
        try {
            List<String> updates = new ArrayList<String>();

            logHeader2("ensuring combination of (NM, NMSPC_CD) unique on KRIM_PERM_T and  KRIM_RSP_T...");
            
            // find duplicates
            res = stmt.executeQuery("select count(*), NM, NMSPC_CD from KRIM_PERM_T group by NM, NMSPC_CD having count(*) > 1");

            //tack perm_id to name to make unique
            while (res.next()) {
                String nm = res.getString(2);
                String nmspccd = res.getString(3);
                updates.add("update KRIM_PERM_T set nm = nm || '[' || perm_id || ']' where nm = '" + nm + "' and nmspc_cd = '" + nmspccd + "'");
            }

            res.close();

            // find duplicates
            res = stmt.executeQuery("select count(*), NM, NMSPC_CD from  KRIM_RSP_T group by NM, NMSPC_CD having count(*) > 1");

            //tack rsp_id to name to make unique
            while (res.next()) {
                String nm = res.getString(2);
                String nmspccd = res.getString(3);
                updates.add("update KRIM_RSP_T set nm = nm || '[' || rsp_id || ']' where nm = '" + nm + "' and nmspc_cd = '" + nmspccd + "'");
            }

            for (String sql : updates) {
				LOGGER.info("executing: " + sql);
                stmt.executeUpdate(sql);
            }

            res.close();
            retval = true;
        } catch (Exception ex) {
			LOGGER.error(ex);
        } finally {
            closeDbObjects(null, null, res);
        }

        return retval;
    }

	/**
	 * @param nm
	 *            {@link String} to process
	 * @return <code>true</code> if <code>nm</code> is non-null and non-empty
	 *         and contains the substring "<code>method:</code>";
	 *         <code>false</code> otherwise
	 */
    private boolean isMethodCall(String nm) {
        return (StringUtils.isNotBlank(nm) && nm.contains("method:"));
    }

	/**
	 * @param nm
	 *            {@link String} of the method name.
	 * @param conn
	 *            {@link Connection} to the database
	 * @param stmt
	 *            {@link Statement} to use to execute SQL
	 * @return <code>true</code> if the specified method was executed
	 *         successfully; <code>false</code> otherwise
	 */
	/*
	 * FIXME this seems to be some way of injecting the method name
	 * "ensureNmNmspccdUnique" into the "upgrade-files" property in the App
	 * properties to modify the code path..... Why? Aside from that it's a dirty
	 * commit/rollback logic block. This should be evaluated and either
	 * corrected or removed.
	 */
    private boolean callMethod(String nm, Connection conn, Statement stmt) {
        boolean retval = false;
        if (StringUtils.isNotBlank(nm)) {
            if (nm.contains("ensureNmNmspccdUnique")) {
                retval = ensureNmNmspccdUnique(conn, stmt);
            }
			/*
			 * FIXME unsafe commit and rollback; don't know if we actually have
			 * done anything, and there may be other updates sitting in the
			 * transaction that shouldn't be interfered with here
			 */

            if (retval) {
                doCommit(conn);
				LOGGER.info("-- Making KRIM_PERM_T or KRIM_RSP_T Unique Succeeded so Committing changes --");
            } else {
                doRollback(conn);
				LOGGER.error("-- Making KRIM_PERM_T or KRIM_RSP_T Unique Failed so RollingBack changes --");
            }
        }

        return retval;
    }
    
	/**
	 * Copies values from <code>fp_prcrmnt_card_hldr_dtl_t</code> to
	 * <code>fp_prcrmnt_card_dflt_t</code>. All updates are done in a single
	 * transaction; if any {@link Exception}s are encountered the entire
	 * transaction is rolled back.
	 * 
	 * @param conn
	 *            {@link Connection} to the database
	 */
	/*
	 * TODO this SQL should be externalized
	 */
    private void populateProcurementCardTable(Connection conn) {
        Statement stmt = null;
        PreparedStatement pstmt = null;
        ResultSet res = null;
        logHeader2("Populating procurement card default table with UA detail data");
        
        try {
            StringBuilder sql = new StringBuilder(512);
            
            sql.append("insert into fp_prcrmnt_card_dflt_t (");
            sql.append("ID,"); // 1
            sql.append("CC_NBR,"); // 2
            sql.append("CC_LAST_FOUR,"); // 3
            sql.append("VER_NBR,"); // 4
            sql.append("OBJ_ID,"); // 5
            sql.append("CARD_HLDR_NM,"); // 6
            sql.append("CARD_HLDR_ALTRNT_NM,"); // 7
            sql.append("CARD_HLDR_LN1_ADDR,"); // 8
            sql.append("CARD_HLDR_LN2_ADDR,"); // 9
            sql.append("CARD_HLDR_CTY_NM,"); // 10
            sql.append("CARD_HLDR_ST_CD,"); // 11
            sql.append("CARD_HLDR_ZIP_CD,"); // 12
            sql.append("CARD_HLDR_WRK_PHN_NBR,"); // 13
            sql.append("CARD_LMT,"); // 14
            sql.append("CARD_CYCLE_AMT_LMT,"); // 15
            sql.append("CARD_CYCLE_VOL_LMT,"); // 16
            sql.append("CARD_MONTHLY_NUMBER,"); // 17
            sql.append("CARD_STAT_CD,"); // 18
            sql.append("CARD_NTE_TXT,"); // 19
            sql.append("FIN_COA_CD,"); // 20
            sql.append("ACCOUNT_NBR,"); // 21
            sql.append("SUB_ACCT_NBR,"); // 22
            sql.append("FIN_OBJECT_CD,"); // 23
            sql.append("FIN_SUB_OBJ_CD,"); // 24
            sql.append("PROJECT_CD,"); // 25
            sql.append("ORG_CD,"); // 26
            sql.append("CARD_HLDR_NET_ID,"); // 27
            sql.append("CARD_GRP_ID,"); // 28
            sql.append("CARD_CANCEL_CD,"); // 29
            sql.append("CARD_OPEN_DT,"); // 30
            sql.append("CARD_CANCEL_DT,"); // 31
            sql.append("CARD_EXPIRE_DT,"); // 32
            sql.append("ROW_ACTV_IND"); // 33
            sql.append(") values (FP_PRCRMNT_CARD_DFLT_SEQ.nextVal, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            pstmt = conn.prepareStatement(sql.toString());
            stmt = conn.createStatement();
            
            sql.setLength(0);
            sql.append("select ");
            sql.append("CC_NBR,"); // 2
            sql.append("CARD_APPROVE_OFFICIAL,"); // 3
            sql.append("VER_NBR,"); // 4
            sql.append("OBJ_ID,"); // 5
            sql.append("CARD_HLDR_NM,"); // 6
            sql.append("CARD_HLDR_ALTRNT_NM,"); // 7
            sql.append("CARD_HLDR_LN1_ADDR,"); // 8
            sql.append("CARD_HLDR_LN2_ADDR,"); // 9
            sql.append("CARD_HLDR_CTY_NM,"); // 10
            sql.append("CARD_HLDR_ST_CD,"); // 11
            sql.append("CARD_HLDR_ZIP_CD,"); // 12
            sql.append("CARD_HLDR_WRK_PHN_NBR,"); // 13
            sql.append("CARD_LMT,"); // 14
            sql.append("CARD_CYCLE_AMT_LMT,"); // 15
            sql.append("CARD_CYCLE_VOL_LMT,"); // 16
            sql.append("CARD_MONTHLY_NUMBER,"); //17
            sql.append("CARD_STAT_CD,"); // 18
            sql.append("CARD_NTE_TXT,"); // 19
            sql.append("FIN_COA_CD,"); // 20
            sql.append("ACCOUNT_NBR,"); // 21
            sql.append("SUB_ACCT_NBR,"); // 22
            sql.append("FIN_OBJECT_CD,"); // 23
            sql.append("FIN_SUB_OBJ_CD,"); // 24
            sql.append("null,"); // 25
            sql.append("ORG_CD,"); //26
            sql.append("CARD_HLDR_NET_ID,"); //27
			sql.append("CARD_GRP_ID,"); //28
			sql.append("CARD_CANCEL_CD,"); //29
			sql.append("CARD_OPEN_DT,"); //30
			sql.append("CARD_CANCEL_DT,"); //31
			sql.append("CARD_EXPIRE_DT,"); //32
            sql.append("'Y'"); // 33
            sql.append("from fp_prcrmnt_card_hldr_dtl_t");
            
            res = stmt.executeQuery(sql.toString());
            
            ResultSetMetaData rmd = res.getMetaData();
            
            int cnt = 0;
            while (res.next()) {
                for (int i = 0; i < rmd.getColumnCount(); ++i) {
                    pstmt.setObject(i+1, res.getObject(i+1));
                }
                
                try {
                    pstmt.executeUpdate();
                }
                
                catch (SQLException ex) {
					LOGGER.error("error on record cc_nbr=" + res.getString("CC_NBR") + " - " + ex.toString(), ex);
                }
                
                if (((cnt++) % 1000) == 0) {
					LOGGER.info(Integer.toString(cnt));
                }
            }
            
            conn.commit();
        }
        
        catch (Exception ex) {
			LOGGER.error(ex);
            try {
                conn.rollback();
            }
            
            catch (Exception ex2) {
				LOGGER.error(ex);
            };
        }
        
        finally {
            closeDbObjects(null, stmt, res);
            closeDbObjects(null, pstmt, null);
        }
    }
    
	/**
	 * Queries <code>krns_maint_doc_t</code> for <code>DOC_CNTNT</code> and
	 * converts it using a {@link MaintainableXMLConversionServiceImpl}
	 * instance. If the <code>encryption-key</code> property is set, will use an
	 * {@link EncryptionService} to decrypt the XML contents from
	 * <code>krns_maint_doc_t</code> and encrypt the transformed xml.
	 * <p>
	 * All updates are done in a single transaction; if any {@link Exception}s
	 * are encountered, the full transaction is rolled back.
	 * 
	 * @param upgradeConn
	 *            {@link Connection} to the upgrade database
	 */
    private void convertMaintenanceDocuments(Connection upgradeConn) {
      logHeader2("Converting legacy maintenance documents to rice 2.0...");
      MaintDocConverter converter = getMaintDocConverter(upgradeConn);
      try {
        MaintDocResult result = converter.get();
        String throughput = getThroughputInSeconds(result.getElapsed(), result.getConverted(), "docs/second");
        LOGGER.info(format("maintenance docs converted -> %s", getCount(result.getConverted())));
        LOGGER.info(format("maintenance docs errors ----> %s", getCount(result.getErrors())));
        LOGGER.info(format("maintenance docs elapsed ---> %s", getTime(result.getElapsed())));
        LOGGER.info(format("maintenance doc throughput -> %s", throughput));
      } catch(Exception e) {
        LOGGER.error(e);
      }
    }
    
    private MaintDocConverter getMaintDocConverter(Connection upgradeConn) {
      try {
        File rules = new File(properties.getProperty("maintenance-document-conversion-rules-file"));
        checkArgument(rules.isFile(),"rules file does not exist -> %s", rules);
        MaintainableXMLConversionServiceImpl converter = new MaintainableXMLConversionServiceImpl(rules);
        EncryptionService encryptor = new EncryptionService(properties.getProperty("encryption-key"));
        int threads = new ThreadsProvider(properties).get();
        LOGGER.info(format("maintenance doc threads -> %s", threads));
        ExecutorService executor = new ExecutorProvider("mdoc", threads).get();
        MaintDocConverter.Builder builder = MaintDocConverter.builder();
        builder.withBatchSize(MAINTENANCE_DOCUMENT_UPDATE_BATCH_SIZE);
        builder.withConnection(upgradeConn);
        builder.withConverter(converter);
        builder.withEncryptor(encryptor);
        builder.withExecutor(executor);
        return builder.build();
      }catch(Exception e) {
        throw new IllegalStateException(e);
      }
    }

	/**
	 * @param directory
	 *            {@link File} representing a directory containing {@link File}s
	 *            to be processed.
	 * @param processedFiles
	 *            {@link Set} of {@link File}s that were actually processed
	 * @return Unmodifiable {@link Set} of any {@link File}s that were in the
	 *         provided <code>directory</code> but not contained in
	 *         <code>processedFiles</code>.
	 */
	public static Set<File> getUnprocessedFiles(File directory, Set<File> processedFiles){
    	Set<File> unprocessed = new HashSet<File>();
    	File[] children = directory.listFiles();
    	for(File child : children){
			if (!processedFiles.contains(child)) {
				unprocessed.add(child);
			}
    	}
		return Collections.unmodifiableSet(unprocessed);
    }

	protected File getPostUpgradeDirectory() {
		return postUpgradeDirectory;
	}
}
