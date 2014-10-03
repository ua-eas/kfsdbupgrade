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
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import liquibase.FileSystemFileOpener;
import liquibase.Liquibase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;


public class App {
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String UNDERLINE = "--------------------------------------------------------------------------------------------------------------------------";
    private static final String ERROR = "************************************************* error *************************************************";
    private static final String HEADER1 = "================================================ ? ================================================";
    private static String upgradeRoot;
    private static List<String> upgradeFolders;
    private static Properties properties;
    private static String propertiesFileName;
    private static Map<String, List<String>> upgradeFiles;

    public static void main(final String args[]) {
        if (args.length == 1) {
            properties = loadProperties(args[0]);
            if (properties != null) {
                propertiesFileName = args[0];
                upgradeRoot = properties.getProperty("upgrade-base-directory");
                upgradeFolders = loadList(properties.getProperty("upgrade-folders"));
                upgradeFiles = loadFolderFileMap("files-");
                
                Connection conn = null;
                Statement stmt = null;
                boolean success = false;
                try {
                    conn = getConnection();
                    stmt = conn.createStatement();
                    writeOut("Starting KFS database upgrade process...");
                    writeOut("");
                    if (doInitialProcessing(stmt)) {
                        if (doUpgrade(conn, stmt)) {
                            success = true;
                        }
                    }
                    
                    if (success) {
                        doCommit(conn);
                        writeOut("");
                        writeHeader1("upgrade completed successfully");
                    }
                }

                catch (Exception ex) {
                    writeOut(ex);
                }

                finally {
                    closeDbObjects(conn, stmt, null);
                }
            } else {
                System.out.println("invalid properties file: " + args[0]);
            }
            
        } else {
            System.out.println("usage: java -Xmx250m -jar kfsdbupgrade.jar <property-file-path>");
        }
    }
    
    private static List <String> loadList(String input) {
        List <String> retval = new ArrayList<>();
        if (StringUtils.isNotBlank(input)) {
            StringTokenizer st = new StringTokenizer(input, ",");
            
            while (st.hasMoreTokens()) {
                retval.add(st.nextToken().trim());
            }
        }
        return retval;
    }
    
    private static Map<String, List<String>> loadFolderFileMap(String prefix) {
        Map <String, List<String>> retval = new HashMap<>();
        
        for (Entry e : properties.entrySet()) {
            String key = (String)e.getKey();
            if (key.startsWith(prefix)) {
                String folder = key.substring(prefix.length());
                retval.put(folder, loadList((String)e.getValue()));
            }
        }
        
        return retval;
    }
    
    private static Properties loadProperties(String fname) {
        Properties retval = null;
        FileReader reader = null;
        
        try {
            reader = new FileReader(fname);
            
            retval = new Properties();
            retval.load(reader);
            if (StringUtils.isBlank(retval.getProperty("database-url"))) {
                retval = null;
            }
        }
        
        catch (Exception ex) {
            retval = null;
        }
    
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            
            catch (Exception ex) {};
        }
        
        return retval;
    }

    private static Set <File> getProcessedFiles() {
        Set <File> retval = new HashSet<>();
        String fname = properties.getProperty("last-good-kfs-file");
        
        if (StringUtils.isNotBlank(fname)) {
            File lastGoodKfsFile = new File(fname);
            if (lastGoodKfsFile.isFile() && lastGoodKfsFile.exists()) {
                boolean foundit = false;
                for (int i = 0; !foundit && (i < upgradeFolders.size()); ++i) {
                    String folder = upgradeFolders.get(i);
                    for (String fileName : upgradeFiles.get(folder)) {
                        File f = new File(upgradeRoot + "/" + folder + "/" + fileName);

                        if (!lastGoodKfsFile.equals(f)) {
                            retval.add(f);
                        } else {
                            foundit = true;
                            break;
                        }
                    }
                }
            }
        }
        
        return retval;
    }

    private static void doRollback(Connection conn) {
        try {
            conn.rollback();
        } 

        catch (Exception ex) {
            writeOut(ex);
        }
    }
    
    private static boolean doCommit(Connection conn) {
        boolean retval = true;
        try {
            conn.commit();
        } 

        catch (Exception ex) {
            writeOut(ex);
            retval = false;
        }
        
        return retval;
    }
    
    private static boolean runSqlFile(Connection conn, Statement stmt, File f, String delimiter) {
        boolean retval = true;
        writeHeader2("processing sql file " + f.getPath());
        List <String> sqlStatements = getSqlStatements(f);
        
        if (!sqlStatements.isEmpty()) {
            for (String sql : sqlStatements) {
                writeLog(sql);
                if (!executeSql(conn, stmt, sql)) {
                    retval = false;
                    break;
                }
            }
        
            if (retval) {
                if ("file".equals(properties.getProperty("commit-level"))) {
                    retval = doCommit(conn);
                }
            } else {
                doRollback(conn);
            }
        } else {
            retval = false;
            writeOut(new Exception("no sql statements found"));
        }
        
        return retval;
    }
    
    private static List <String> getSqlStatements(File f) {
        List <String> retval = new ArrayList<>();
        LineNumberReader lnr = null;
        
        try {
            lnr = new LineNumberReader(new FileReader(f));
            String line = null;
            StringBuilder sql = new StringBuilder(512);
            
            while ((line = lnr.readLine()) != null) {
                if (StringUtils.isNotBlank(line) && !line.trim().startsWith("--")) {
                    line = line.trim();
                    if (line.equals("/")) {
                        if (sql.length() > 0) {
                            retval.add(sql.toString());
                            sql.setLength(0);
                        } 
                    } else if (line.endsWith("/") || line.endsWith(";")) {
                        sql.append(" ");
                        sql.append(line.substring(0, line.length()-1));
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
        }
        
        catch (Exception ex) {
            writeOut(ex);
        }
        
        finally {
            try {
                if (lnr != null) {
                    lnr.close();
                }
            }
            
            catch (Exception ex) {}
        }
        
        return retval;
    }
    
    private static File getUpgradeFile(String fname) {
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
    
    private static boolean doUpgrade(Connection conn, Statement stmt) {
        boolean retval = true;
        writeHeader1("upgrading kfs");
        Set <File> processedKfsFiles = getProcessedFiles();
        
        for (int i = 0; retval && (i < upgradeFolders.size()); ++i) {
            String folder = upgradeFolders.get(i);
            writeHeader2("processing folder " + folder);
            
            for (String fname : upgradeFiles.get(folder)) {
                File f = getUpgradeFile(upgradeRoot + "/" + folder + "/" + fname);
                
                if (!processedKfsFiles.contains(f)) {
                    if (f.getName().endsWith(".sql")) {
                        if (!runSqlFile(conn, stmt, f, ";")) {
                            retval = false;
                            writeProcessedFileInfo("[failure] " + f.getPath());
                        } else {
                            writeProcessedFileInfo("[success] " + f.getPath());
                        }
                    } else {
                        if(!runLiquibase(conn, f)) {
                            retval = false;
                            writeProcessedFileInfo("[failure] " + f.getPath());
                        } else {
                            writeProcessedFileInfo("[success] " + f.getPath());
                        }
                    }

                    if (!retval) {
                        break;
                    } 
                }
            }
            
            if (retval && !"file".equals(properties.getProperty("commit-level"))) {
                retval = doCommit(conn);
            }

        }
        
        return retval;
    }
    
    private static boolean runLiquibase(Connection conn, File f) {
        boolean retval = true;
        StringWriter sw = new StringWriter();
        writeHeader2("processing liquibase file " + f.getPath());

        try {
            Liquibase liquibase = new Liquibase(f.getName(), new FileSystemFileOpener(f.getParentFile().getPath()), conn);
            liquibase.reportStatus(true, null, sw);
            liquibase.update(null);
            retval = true;
            writeOut(sw.toString());
        }
        
        catch (Exception ex) {
            retval= false;
            writeOut(sw.toString());
            writeOut(ex);
        }
        
        return retval;
    }

    private static void writeOut(Exception ex) {
        System.out.println();
        System.out.println(ERROR);
        ex.printStackTrace(System.out);
        writeLog(ex);
    }
    
    private static void writeLog(Exception ex) {
        PrintWriter pw = null;
        
        try {
            pw = getOutputLogWriter();
            pw.println();
            pw.println(ERROR);
            ex.printStackTrace(pw);
        }
        
        catch (Exception ex2) {
        }
        
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            
            catch (Exception ex2) {};
        }
    }

    private static void writeOut(String msg) {
        System.out.println(msg);
        writeLog(msg);
    }
    
    private static void writeLog(String msg) {
        PrintWriter pw = null;
        
        try {
            pw = getOutputLogWriter();
            if (StringUtils.isNotBlank(msg)) {
                pw.println(getTimeString() + msg);
            } else {
                pw.println();
            }
        }
        
        catch (Exception ex2) {
        }
        
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            
            catch (Exception ex2) {};
        }
    }

    private static String getTimeString() {
        return "[" + DF.format(new Date()) + "] ";
    }
    
    private static Connection getConnection() throws Exception {
		Connection retval = null;
		String url = properties.getProperty("database-url");
        String user = properties.getProperty("database-user");
    	String pass = properties.getProperty("database-password");
		
        writeOut("");
        writeOut("Connecting to db " + properties.getProperty("database-name") + "...");
        writeOut("url=" + url);
			
        Class.forName(properties.getProperty("database-driver"));
		retval = DriverManager.getConnection(url, user, pass);
			
		retval.setReadOnly(false);
        retval.setAutoCommit(false);
			
		writeOut("connected to database " + properties.getProperty("database-name"));
		writeOut("");
        
		return retval;
	}
	
	
	private static void closeDbObjects(Connection conn, Statement stmt, ResultSet res) {
		try {
			if (res != null) {
				res.close();
			}
		}
		
		catch (Exception ex) {};
		try {
			if (stmt != null) {
				stmt.close();
			}
		}
		
		catch (Exception ex) {};
		try {
			if (conn != null) {
				conn.close();
			}
		}
		
		catch (Exception ex) {};
	}


    private static boolean executeSql(Connection conn, Statement stmt, String sql) {
        boolean retval = false;
        
        try {
            if (isDDL(sql)) {
                stmt.execute(sql);
            } else {
                stmt.executeUpdate(sql);
            }
            
            retval = true;
        }
        
        catch (Exception ex) {
            writeOut(ex);
        }
        
        return retval;
    }
    
    private static boolean isDDL(String sql) {
        String s = sql.toUpperCase();
        return (!s.toUpperCase().startsWith("UPDATE") && !s.startsWith("INSERT") && !s.startsWith("DELETE"));
    }

    private static void deleteFile(File f) throws IOException {
        try {
            FileUtils.forceDelete(f);
        }
        
        catch (FileNotFoundException ex) {};
    }
    
    private static boolean doInitialProcessing(Statement stmt) {
        boolean retval = false;
        ResultSet res = null;
        try {
            deleteFile(new File(properties.getProperty("output-log-file-name")));
            deleteFile(new File(properties.getProperty("processed-files-file-name")));
            
            writeHeader1("pre-upgrade processing");
            writeHeader2("dropping materialized view logs..");
            res = stmt.executeQuery("select LOG_OWNER || '.' || MASTER from SYS.user_mview_logs");
            
            List <String> logs = new ArrayList<>();
            
            while (res.next()) {
                logs.add(res.getString(1));
            }

            for (String log : logs) {
                stmt.execute("drop materialized view log on " + log);
                writeOut("dropped materialized view log on " + log);
            }

            retval = true;
        }
        
        catch (Exception ex) {
            writeOut(ex);
        }
        
        finally {
            closeDbObjects(null, null, res);
        }
        
        return retval;
    }
    
    private static void writeHeader1(String msg) {
        writeOut("");
        writeOut(HEADER1.replace("?", msg));
        writeOut("");
    }

    private static void writeHeader2(String msg) {
        writeOut("");
        writeOut(msg);
        writeOut(UNDERLINE);
        writeOut("");
    }
    
    private static PrintWriter getOutputLogWriter() throws IOException {
        return new PrintWriter(new FileWriter(properties.getProperty("output-log-file-name"), true));
    }

    private static PrintWriter getProcessedFilesWriter() throws IOException {
        return new PrintWriter(new FileWriter(properties.getProperty("processed-files-file-name"), true));
    }
    
    private static void writeProcessedFileInfo(String txt) {
        PrintWriter pw = null;
        try {
            pw = getProcessedFilesWriter();
            pw.println(txt);
        }
        
        catch (Exception ex) {
            writeOut(ex);
        }
        
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            
            catch (Exception ex) {};
        }
    }
}
