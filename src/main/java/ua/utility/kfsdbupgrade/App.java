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

import java.io.File;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import liquibase.FileSystemFileOpener;
import liquibase.Liquibase;

public class App {
    private static final int MAINTENANCE_DOCUMENT_UPDATE_BATCH_SIZE = 1000;
    
	public static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final String UNDERLINE = "--------------------------------------------------------------------------------------------------------------------------";
    public static final String ERROR = "************************************************* error *************************************************";
    public static final String HEADER1 = "================================================ ? ================================================";
    private static final String INDEX_NAME_TEMPLATE = "[table-name]I{index}";
	/**
	 * Populated by the <code>upgrade-base-directory</code> {@link Properties}
	 * entry
	 */
    private static String upgradeRoot;
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
	 * Main program entry point. Single argument is expected of a path to the
	 * <code>kfsdbupgrade.properties</code> properties file. Optional second
	 * argument of "<code>ingestWorkflow</code>" if the ingest workflow code
	 * path should be followed instead of the database upgrade code path.
	 * 
	 * @param args
	 */
    public static void main(final String args[]) {
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

    /**
	 * Constructor.
	 * 
	 * @param propertyFileName
	 *            {@link String} of the <code>.properties</code> file location
	 *            to use
	 */
	public App(String propertyFileName) {
        properties = loadProperties(propertyFileName);
        if (properties != null) {
            upgradeRoot = properties.getProperty("upgrade-base-directory");
            upgradeFolders = loadList(properties.getProperty("upgrade-folders"));
            upgradeFiles = loadFolderFileMap("files-");
        } else {
            writeLog("invalid properties file: " + propertyFileName);            
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
            writeLog("Starting KFS database upgrade process...");
            writeLog("");

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
					writeLog("dropTempTables(conn2, stmt); -- FAILED in doUpgrade()");
					writeLog(e);
				}
				try {
					runMiscSql(conn2, stmt);
				} catch (Exception e) {
					writeLog("runMiscSql(conn2, stmt); -- FAILED in doUpgrade()" );
					writeLog(e);
				}
				try {
					populateProcurementCardTable(conn1);
				} catch (Exception e) {
					writeLog("populateProcurementCardTable(conn1); -- FAILED in doUpgrade() " );
					writeLog(e);
				}
				try {
					updatePurchasingStatuses(conn1);
				} catch (Exception e) {
					writeLog("updatePurchasingStatuses(conn1); -- FAILED in doUpgrade() " );
					writeLog(e);
				}
				try {
					createExistingIndexes(conn2, stmt);
				} catch (Exception e) {
					writeLog("createExistingIndexes(conn2, stmt); -- FAILED in doUpgrade() " );
					writeLog(e);
				}
				try {
					createPublicSynonyms(conn2, stmt);
				} catch (Exception e) {
					writeLog("createPublicSynonyms(conn2, stmt); -- FAILED in doUpgrade() " );
					writeLog(e);
				}
				try {
					createForeignKeyIndexes(conn2, stmt);
				} catch (Exception e) {
					writeLog("createForeignKeyIndexes(conn2, stmt) -- FAILED in doUpgrade() ");
					writeLog(e);
				}
				try {
					createDocumentSearchEntries(conn2, stmt);
				} catch (Exception e) {
					writeLog("createDocumentSearchEntries(conn2, stmt); -- FAILED in doUpgrade()" );
					writeLog(e);
				}
				if (StringUtils.equalsIgnoreCase(properties.getProperty("run-maintenance-document-conversion"), "true")) {
                    try {
						convertMaintenanceDocuments(conn1);
					} catch (Exception e) {
						writeLog("convertMaintenanceDocuments(conn1); -- FAILED in doUpgrade() " );
						writeLog(e);
					}
                }
                writeLog("");
                writeHeader1Log("upgrade completed successfully");
            }
        } 

        catch (Exception ex) {
            writeLog(ex);
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
        writeHeader2Log("Dropping temporary tables");

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
                    writeLog("failed to drop temp table " + t);
                    writeLog(ex);
                }
            }
        } catch (Exception ex) {
            writeLog(ex);
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
        Properties retval = null;
        FileReader reader = null;

        try {
            reader = new FileReader(fname);

            retval = new Properties();
            retval.load(reader);
            if (StringUtils.isBlank(retval.getProperty("database-url"))) {
                retval = null;
            }
        } catch (Exception ex) {
            retval = null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception ex) {
            };
        }

        return retval;
    }

	/**
	 * Calls {@link Connection#rollback()}, and if any {@link Exception}s are
	 * encountered redirects them to {@link #writeLog(Exception)}.
	 * 
	 * @param conn
	 *            {@link Connection} to rollback.
	 */
    private void doRollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception ex) {
            writeLog(ex);
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
            writeLog(ex);
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
    private boolean runSqlFile(Connection conn, Statement stmt, File f, String delimiter) {
        boolean retval = true;
        writeHeader2Log("processing sql file " + f.getPath());
        List<String> sqlStatements = getSqlStatements(f);

        if (!sqlStatements.isEmpty()) {
            for (String sql : sqlStatements) {
                writeLog(sql);
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
            writeLog(new Exception("no sql statements found"));
        }

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
            writeLog(ex);
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
        writeHeader1Log("upgrading kfs");

        String lastProcessedFile = properties.getProperty("last-processed-file");
		RunRequest runRequest = buildRunRequest(lastProcessedFile);
		List<String> folders = runRequest.getDirectories();

        for (String folder : folders) {
            writeHeader2Log("processing folder " + folder);

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
        writeHeader2Log("processing liquibase file " + f.getPath());
        PrintWriter pw = null;
        try {
            Liquibase liquibase = new Liquibase(f.getName(), new FileSystemFileOpener(f.getParentFile().getPath()), conn);
            liquibase.getDatabase().setAutoCommit(true);
            liquibase.reportStatus(true, null, pw = getOutputLogWriter());
            liquibase.update(null);
            retval = true;
        } catch (Exception ex) {
            retval = false;
            if (pw != null) {
                pw.close();
            }
            pw = null;
            writeLog(ex);
        } finally {
            if (pw != null) {
                pw.close();
            }
        }

        return retval;
    }

	/**
	 * Write out the {@link Exception} and its stacktrace to STDOUT as well as
	 * the logging mechanism
	 * 
	 * @param ex
	 *            {@link Exception} to write out
	 * @see {@link #writeLog(Exception)}
	 */
    public void writeOut(Exception ex) {
        System.out.println();
        System.out.println(getTimeString() + ERROR);
        System.out.println("--" + ex.getClass().getName() + "---===");
        System.out.println("--" + ex.getMessage() + "--");    
        ex.printStackTrace(System.out);
    }

	/**
	 * Write out the {@link Exception} and its stacktrace to the logging
	 * mechanism
	 * 
	 * @param ex
	 *            {@link Exception} to log
	 * 
	 * @see {@link #writeOut(Exception)}
	 */
	public void writeLog(Exception ex) {
        PrintWriter pw = null;
        try {
            pw = getOutputLogWriter();
            pw.println();
            pw.println(getTimeString() + "--" + ERROR);
            pw.println("--" + ex.getClass().getName() + "---===");
            pw.println("--" + ex.getMessage() + "--");            
            ex.printStackTrace(pw);
            writeOut(ex);
        } catch (Exception ex2) {
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex2) {
            };
        }
    }

	/**
	 * Write out the provided {@link String} to STDOUT as well as the logging
	 * mechanism.
	 * 
	 * @param msg
	 *            {@link String} to write out
	 * @see {@link #writeLog(String)}
	 */
    public void writeOut(String msg) {
        System.out.println(getTimeString() + "--" + msg);
    }

	/**
	 * Log the provided {@link String}
	 * 
	 * @param msg
	 *            {@link String} to log
	 * @see {@link #writeLog(String)}
	 */
	public void writeLog(String msg) {
        PrintWriter pw = null;
        try {
            pw = getOutputLogWriter();
            if (StringUtils.isNotBlank(msg)) {
                pw.println(getTimeString() + "--" + msg);
                writeOut(msg);
            } else {
                pw.println();
                writeOut("");
            }
        } catch (Exception ex2) {
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex2) {
            };
        }
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
    private Connection getUpgradeConnection() throws Exception {
        Connection retval = null;
        String url = properties.getProperty("database-url");

        Properties props = new Properties();
        props.setProperty("user", properties.getProperty("database-user"));
        props.setProperty("password", properties.getProperty("database-password"));

        writeLog("Connecting to db " + properties.getProperty("database-name") + "...");
        writeLog("url=" + url);

        Class.forName(properties.getProperty("database-driver"));
        retval = DriverManager.getConnection(url, props);
        retval.setReadOnly(false);
        retval.setAutoCommit(false);

        writeLog("connected to database " + properties.getProperty("database-name"));
        writeLog("");

        return retval;
    }

	/**
	 * @return {@link Connection} to the database with the URL of
	 *         <code>legacy-database-url</code> in this {@link App}s
	 *         {@link Properties} . Uses the <code>legacy-database-user</code>,
	 *         <code>legacy-database-password</code>,
	 *         <code>legacy-database-name</code>, and
	 *         <code>legacy-database-driver</code> {@link Properties} entries
	 *         for connection details. Read-only and auto-commit for the
	 *         {@link Connection} are both set to <code>false</code>.
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown.
	 */
    private Connection getLegacyConnection() throws Exception {
        Connection retval = null;
        String url = properties.getProperty("legacy-database-url");

        Properties props = new Properties();
        props.setProperty("user", properties.getProperty("legacy-database-user"));
        props.setProperty("password", properties.getProperty("legacy-database-password"));

        writeLog("Connecting to db " + properties.getProperty("legacy-database-name") + "...");
        writeLog("url=" + url);

        Class.forName(properties.getProperty("legacy-database-driver"));
        retval = DriverManager.getConnection(url, props);
        retval.setReadOnly(false);
        retval.setAutoCommit(false);

        writeLog("connected to database " + properties.getProperty("legacy-database-name"));
        writeLog("");

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
    private void closeDbObjects(Connection conn, Statement stmt, ResultSet res) {
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
            writeLog(ex);
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

            writeHeader1Log("pre-upgrade processing");
            writeHeader2Log("dropping materialized view logs...");
            res = stmt.executeQuery("select LOG_OWNER || '.' || MASTER from SYS.user_mview_logs");

            List<String> logs = new ArrayList<String>();

            while (res.next()) {
                logs.add(res.getString(1));
            }

            for (String log : logs) {
                stmt.execute("drop materialized view log on " + log);
                writeLog("dropped materialized view log on " + log);
            }

            res.close();

            writeHeader2Log("ensuring combination of (SORT_CD, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ACTV_IND) unique on KRIM_TYP_ATTR_T...");

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
            writeLog(ex);
        } finally {
            closeDbObjects(null, null, res);
            closeDbObjects(null, stmt2, res2);
        }

        return retval;
    }
    
	/**
	 * {@link #writeHeader1Log(String)} the provided <code>message</code> encased in a
	 * block of '='s for emphasis
	 * 
	 * @param msg
	 */    
    public void writeHeader1Log(String msg) {
        writeLog("");
        writeLog(HEADER1.replace("?", getTimeString() + "--" + msg));
        writeLog("");
    }
    
    
	/**
	 * {@link #writeHeader2Log(String)} the provided <code>message</code> followed by a
	 * line of dashes for emphasis
	 * 
	 * @param msg
	 */    
    public void writeHeader2Log(String msg) {
        writeLog("");
        writeLog(getTimeString() + "--" + msg);
        writeLog(UNDERLINE);
        writeLog("");
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
            writeLog(ex);
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
                retval.add(st.nextToken());
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
    private void createExistingIndexes(Connection conn, Statement stmt) {
        LineNumberReader lnr = null;

        writeHeader2Log("creating KFS indexes that existed prior to upgrade where required ");

        try {
            lnr = new LineNumberReader(new FileReader(upgradeRoot + "/post-upgrade/sql/kfs-indexes.sql"));

            String line = null;

            while ((line = lnr.readLine()) != null) {
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
                                writeLog("failed to create index: " + sql.toString());
                                writeLog(ex);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            writeLog(ex);
        } finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception ex) {
            };
        }
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

                columns.add(res.getString(2));
            }

			/*
			 * for each index name, if the list of columns in that index is the
			 * same size of the input columnNames, AND the contents are the same
			 * (NOTE: here, also matching on order; don't strictly need to,
			 * potential bug; looking for equivalence, not strict equality),
			 * then the index exists
			 */
            for (List<String> columns : map.values()) {
                if (columns.size() == columnNames.size()) {
                    boolean foundit = true;
                    for (int i = 0; i < columns.size(); ++i) {
                        if (!columns.get(i).equals(columnNames.get(i))) {
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
	// FIXME only exception handling is to log out exception... very brittle
    private void createPublicSynonyms(Connection conn, Statement stmt) {
        LineNumberReader lnr = null;

        writeHeader2Log("creating KFS public synonyms that existed prior to upgrade where required ");

        try {
            lnr = new LineNumberReader(new FileReader(upgradeRoot + "/post-upgrade/sql/kfs-public-synonyms.sql"));

            String line = null;

            while ((line = lnr.readLine()) != null) {
                String synonymName = getSynonymName(line);

                if (!synonymExists(conn, stmt, synonymName)) {
                    try {
                        stmt.execute(line);
                    } catch (SQLException ex) {
                    	writeLog(ex);
                        writeLog("failed to create public synonym: " + line);
                    }
                }
            }
        } catch (Exception ex) {
            writeLog(ex);
        } finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception ex) {
            	writeLog(ex);
            };
        }
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
            writeLog(ex);
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
        writeHeader2Log("creating indexes on foreign keys where required...");
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
                    writeLog("creating required foreign key indexes on table " + tname + "...");
                    int cnt = 0;
                    for (String sql : sqllist) {
                        try {
                            stmt.executeQuery(sql);
                            cnt++;
                        } catch (Exception ex) {
                            writeLog("create index failed: " + sql);
                        }
                    }

                    writeLog("    " + cnt + " indexes created");
                }
            }
        } catch (Exception ex) {
            writeLog(ex);
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
                pstmt.executeUpdate();

                if ((i % 10000) == 0) {
                    writeLog(i + " krew_doc_hdr_ext_t entries inserted");
                }

                i++;
            }

            conn.commit();
        } catch (Exception ex) {
        	writeLog(ex);

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex2) {
                	writeLog(ex);
                };
            }
        } finally {
            try {
                closeDbObjects(null, pstmt, res);
            } catch (Exception ex) {
            	writeLog(ex);
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

        writeHeader2Log("Executiong miscellaneous post-upgrade sql");
        try {
            lnr = new LineNumberReader(new FileReader(upgradeRoot + "/post-upgrade/sql/misc.sql"));

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
                        writeLog(sql);
                    } catch (SQLException ex) {
                        writeLog("sql execution failed: " + sql);
                        writeLog(ex);
                    }
                }
            }
        } catch (Exception ex) {
            writeLog(ex);
        } finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception ex) {
            	writeLog(ex);
            };
        }
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
        Connection legacyConn = null;
        Statement legacyStmt = null;
        ResultSet legacyRes = null;
        PreparedStatement upgradeStmt1 = null;
        PreparedStatement upgradeStmt2 = null;
        try {
            // load status names from legacy status tables
            legacyConn = getLegacyConnection();
            legacyStmt = legacyConn.createStatement();

            legacyRes = legacyStmt.executeQuery("select CRDT_MEMO_STAT_CD, CRDT_MEMO_STAT_DESC from AP_CRDT_MEMO_STAT_T");

            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from AP_CRDT_MEMO_T where DEPR_CRDT_MEMO_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from AP_CRDT_MEMO_T where DEPR_CRDT_MEMO_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

                writeLog("updating credit memo app_doc_stat[" + desc + "] in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

                writeLog("updating credit memo app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            closeDbObjects(null, upgradeStmt1, legacyRes);
            closeDbObjects(null, upgradeStmt2, null);

            legacyRes = legacyStmt.executeQuery("select PMT_RQST_STAT_CD, PMT_RQST_STAT_DESC from AP_PMT_RQST_STAT_T");
            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from AP_PMT_RQST_T where DEPR_PMT_RQST_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from AP_PMT_RQST_T where DEPR_PMT_RQST_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

                writeLog("updating payment request app_doc_stat[" + desc + "]  in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

                writeLog("updating payment request app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            closeDbObjects(null, upgradeStmt1, legacyRes);
            closeDbObjects(null, upgradeStmt2, null);

            legacyRes = legacyStmt.executeQuery("select PO_STAT_CD, PO_STAT_DESC from PUR_PO_STAT_T");
            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from PUR_PO_T where DEPR_PO_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from PUR_PO_T where DEPR_PO_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

                writeLog("updating purchase order app_doc_stat[" + desc + "]  in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

                writeLog("updating purchase order app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            closeDbObjects(null, upgradeStmt1, legacyRes);
            closeDbObjects(null, upgradeStmt2, null);

            legacyRes = legacyStmt.executeQuery("select RCVNG_LN_STAT_CD, RCVNG_LN_STAT_DESC from PUR_RCVNG_LN_STAT_T");
            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from PUR_RCVNG_LN_T where DEPR_RCVNG_LN_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from PUR_RCVNG_LN_T where DEPR_RCVNG_LN_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

                writeLog("updating purchase receiving line app_doc_stat[" + desc + "]  in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

                writeLog("updating purchase receiving line app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            closeDbObjects(null, upgradeStmt1, legacyRes);
            closeDbObjects(null, upgradeStmt2, null);

            legacyRes = legacyStmt.executeQuery("select REQS_STAT_CD, REQS_STAT_DESC from PUR_REQS_STAT_T");
            upgradeStmt1 = upgradeConn.prepareStatement("update krew_doc_hdr_t set app_doc_stat = ? where doc_hdr_id in (select fdoc_nbr from PUR_REQS_T where DEPR_REQS_STAT_CD = ?)");
            upgradeStmt2 = upgradeConn.prepareStatement("update fs_doc_header_t set app_doc_stat = ? where fdoc_nbr in (select fdoc_nbr from PUR_REQS_T where DEPR_REQS_STAT_CD = ?)");
            while (legacyRes.next()) {
                String cd = legacyRes.getString(1);
                String desc = legacyRes.getString(2);

                writeLog("updating requisition app_doc_stat[" + desc + "]  in krew_doc_hdr_t...");
                upgradeStmt1.setString(1, desc.replace("&", "and"));
                upgradeStmt1.setString(2, cd);
                upgradeStmt1.executeUpdate();

                writeLog("updating requisition app_doc_stat[" + desc + "]  in fs_doc_header_t...");
                upgradeStmt2.setString(1, desc.replace("&", "and"));
                upgradeStmt2.setString(2, cd);
                upgradeStmt2.executeUpdate();
            }

            upgradeConn.commit();
        } catch (Exception ex) {
            writeLog(ex);
            try {
                upgradeConn.rollback();
            } catch (Exception ex2) {
            };
        } finally {
            closeDbObjects(legacyConn, legacyStmt, legacyRes);
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

            writeHeader2Log("ensuring combination of (NM, NMSPC_CD) unique on KRIM_PERM_T and  KRIM_RSP_T...");
            
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
                writeLog("executing: " + sql);
                stmt.executeUpdate(sql);
            }

            res.close();
            retval = true;
        } catch (Exception ex) {
            writeLog(ex);
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
                writeLog("-- Making KRIM_PERM_T or KRIM_RSP_T Unique Succeeded so Committing changes --");
            } else {
                doRollback(conn);
                writeLog("-- Making KRIM_PERM_T or KRIM_RSP_T Unique Failed so RollingBack changes --");
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
        writeHeader2Log("Populating procurement card default table with UA detail data");
        
        try {
            StringBuilder sql = new StringBuilder(512);
            
            sql.append("insert into fp_prcrmnt_card_dflt_t (");
            sql.append("ID,"); // 1
            sql.append("CC_NBR,"); // 2
            sql.append("VER_NBR,"); // 3
            sql.append("OBJ_ID,"); // 4
            sql.append("CARD_HLDR_NM,"); // 5
            sql.append("CARD_HLDR_ALTRNT_NM,"); // 6
            sql.append("CARD_HLDR_LN1_ADDR,"); // 7
            sql.append("CARD_HLDR_LN2_ADDR,"); // 8
            sql.append("CARD_HLDR_CTY_NM,"); // 9
            sql.append("CARD_HLDR_ST_CD,"); // 10
            sql.append("CARD_HLDR_ZIP_CD,"); // 11
            sql.append("CARD_HLDR_WRK_PHN_NBR,"); // 12
            sql.append("CARD_LMT,"); // 13
            sql.append("CARD_CYCLE_AMT_LMT,"); // 14
            sql.append("CARD_CYCLE_VOL_LMT,"); // 15
            sql.append("CARD_STAT_CD,"); // 16
            sql.append("CARD_NTE_TXT,"); // 17
            sql.append("FIN_COA_CD,"); // 18
            sql.append("ACCOUNT_NBR,"); // 19
            sql.append("SUB_ACCT_NBR,"); // 20
            sql.append("FIN_OBJECT_CD,"); // 21
            sql.append("FIN_SUB_OBJ_CD,"); // 22
            sql.append("PROJECT_CD,"); // 23
            sql.append("ROW_ACTV_IND"); // 24
            sql.append(") values (FP_PRCRMNT_CARD_DFLT_SEQ.nextVal, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            pstmt = conn.prepareStatement(sql.toString());
            stmt = conn.createStatement();
            
            sql.setLength(0);
            sql.append("select ");
            sql.append("CC_NBR,"); // 2
            sql.append("VER_NBR,"); // 3
            sql.append("OBJ_ID,"); // 4
            sql.append("CARD_HLDR_NM,"); // 5
            sql.append("CARD_HLDR_ALTRNT_NM,"); // 6
            sql.append("CARD_HLDR_LN1_ADDR,"); // 7
            sql.append("CARD_HLDR_LN2_ADDR,"); // 8
            sql.append("CARD_HLDR_CTY_NM,"); // 9
            sql.append("CARD_HLDR_ST_CD,"); // 10
            sql.append("CARD_HLDR_ZIP_CD,"); // 11
            sql.append("CARD_HLDR_WRK_PHN_NBR,"); // 12
            sql.append("CARD_LMT,"); // 13
            sql.append("CARD_CYCLE_AMT_LMT,"); // 14
            sql.append("CARD_CYCLE_VOL_LMT,"); // 15
            sql.append("CARD_STAT_CD,"); // 16
            sql.append("CARD_NTE_TXT,"); // 17
            sql.append("FIN_COA_CD,"); // 18
            sql.append("ACCOUNT_NBR,"); // 19
            sql.append("SUB_ACCT_NBR,"); // 20
            sql.append("FIN_OBJECT_CD,"); // 21
            sql.append("FIN_SUB_OBJ_CD,"); // 22
            sql.append("null,"); // 23
            sql.append("'Y'"); // 24
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
                    writeLog("error on record cc_nbr=" + res.getString("CC_NBR") + " - " + ex.toString());
                    writeLog(ex);
                }
                
                if (((cnt++) % 1000) == 0) {
                    writeLog(Integer.toString(cnt));
                }
            }
            
            conn.commit();
        }
        
        catch (Exception ex) {
            writeLog(ex);
            try {
                conn.rollback();
            }
            
            catch (Exception ex2) {
            	writeLog(ex);
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
        PreparedStatement pstmt = null;
        Statement stmt = null;
        ResultSet res = null;

        try {
            writeHeader2Log("Converting legacy maintenance documents to rice 2.0...");

            String fname = properties.getProperty("maintenance-document-conversion-rules-file");

            if (StringUtils.isNotBlank(fname)) {
                File f = new File(fname);

                if (f.exists()) {
                    MaintainableXMLConversionServiceImpl maintainableXMLConversionServiceImpl = new MaintainableXMLConversionServiceImpl(this, f);
                    EncryptionService encryptService = new EncryptionService(properties.getProperty("encryption-key"));
                    pstmt = upgradeConn.prepareStatement("update krns_maint_doc_t set DOC_CNTNT = ? where DOC_HDR_ID = ?");
                    stmt = upgradeConn.createStatement();

                    res = stmt.executeQuery("SELECT DOC_HDR_ID, DOC_CNTNT FROM krns_maint_doc_t ");

                    int cnt = 0;
                    long start = System.currentTimeMillis();
                    while (res.next()) {
                        String docid = res.getString(1);
                        String oldXml = null;

                        if (encryptService.isEnabled()) {
                            oldXml = encryptService.decrypt(res.getString(2));
                        } else {
                            oldXml = res.getString(2);
                        }

                        String newXml  = null;

                        try {
                            newXml = maintainableXMLConversionServiceImpl.transformMaintainableXML(oldXml);
                        }

                        catch (Exception ex) {
                            newXml = null;
                            writeLog("error occured while attempting to convert document " + docid);
                            writeLog("-------------------------------------------- xml ---------------------------------------------");
                            writeLog(oldXml);
                            writeLog(ex);
                        }

                        if (newXml != null) {
                            if (encryptService.isEnabled()) {
                                pstmt.setString(1, encryptService.encrypt(newXml));
                            } else {
                                pstmt.setString(1, newXml);
                            }

                            pstmt.setString(2, docid);

                            pstmt.addBatch();

                            cnt++;

                            if ((cnt % MAINTENANCE_DOCUMENT_UPDATE_BATCH_SIZE) == 0) {
                                pstmt.executeBatch();

                                if ((cnt % (5 * MAINTENANCE_DOCUMENT_UPDATE_BATCH_SIZE)) == 0) {
                                    writeLog(cnt + " documents processed - " + ((System.currentTimeMillis() - start)/1000) + "sec");
                                    start = System.currentTimeMillis();
                                }
                            }
                        }
                    }

                    if ((cnt % MAINTENANCE_DOCUMENT_UPDATE_BATCH_SIZE) != 0) {
                        pstmt.executeBatch();
                    }

                    doCommit(upgradeConn);

                    writeLog(cnt + " maintenance documents upgraded.");
                } else {
                    writeLog("maintenance document conversion rules file " + f.getPath() + " does not exist");
                }
            } else {
                writeLog("no property 'maintenance-document-conversion-rules-file' entry in property file");
            }
        }

        catch (Exception ex) {
            doRollback(upgradeConn);
            writeLog(ex);
        }
    }
}
