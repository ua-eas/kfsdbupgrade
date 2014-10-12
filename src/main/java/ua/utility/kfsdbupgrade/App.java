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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
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
    private static Map<String, List<String>> upgradeFiles;
    
    public static void main(final String args[]) {
        if (args.length == 1) {
            properties = loadProperties(args[0]);
            if (properties != null) {
                upgradeRoot = properties.getProperty("upgrade-base-directory");
                upgradeFolders = loadList(properties.getProperty("upgrade-folders"));
                upgradeFiles = loadFolderFileMap("files-");
                
                Connection conn1 = null;
                Connection conn2 = null;
                Statement stmt = null;
                boolean success = false;
                try {
                    conn1 = getConnection();
                    conn2 = getConnection();
                    conn2.setAutoCommit(true);
                    stmt = conn1.createStatement();
                    writeOut("Starting KFS database upgrade process...");
                    writeOut("");
                    if (doInitialProcessing(stmt)) {
                        doCommit(conn1);
                        if (doUpgrade(conn1, conn2, stmt)) {
                            success = true;
                        }
                    }
                    
                    if (success) {
                        doCommit(conn1);
                        createIndexes(conn2, stmt);
                        createPublicSynonyms(conn2, stmt);
                        writeOut("");
                        writeHeader1("upgrade completed successfully");
                    }
                }

                catch (Exception ex) {
                    writeOut(ex);
                }

                finally {
                    closeDbObjects(conn1, stmt, null);
                    closeDbObjects(conn2, null, null);
                }
            } else {
                System.out.println("invalid properties file: " + args[0]);
            }
            
        } else {
            System.out.println("usage: java -Xmx500m -jar kfsdbupgrade.jar <property-file-path>");
        }
    }
    
    private static List <String> loadList(String input) {
        List <String> retval = new ArrayList<String>();
        if (StringUtils.isNotBlank(input)) {
            StringTokenizer st = new StringTokenizer(input, ",");
            
            while (st.hasMoreTokens()) {
                retval.add(st.nextToken().trim());
            }
        }
        return retval;
    }
    
    private static Map<String, List<String>> loadFolderFileMap(String prefix) {
        Map <String, List<String>> retval = new HashMap<String, List<String>>();
        
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
                retval = doCommit(conn);
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
        List <String> retval = new ArrayList<String>();
        LineNumberReader lnr = null;
        
        try {
            lnr = new LineNumberReader(new FileReader(f));
            String line = null;
            StringBuilder sql = new StringBuilder(512);
            
            while ((line = lnr.readLine()) != null) {
                if (StringUtils.isNotBlank(line) && !line.trim().startsWith("--")) {
                    line = line.trim();
                    if (line.equals("/") || line.equals(";")) {
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
    
    private static String getLastProcessedFolder(String lastProcessedFile) {
        String retval = null;
        String s = lastProcessedFile.substring(properties.getProperty("upgrade-base-directory").length() + 1);
        int pos = s.indexOf("/");
        retval = s.substring(0, pos);
        return retval;
    }
    
    private static List <String> getFolders(String lastProcessedFile) {
        List <String> retval = new ArrayList<String>();

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
    
    private static List <String> getFolderFiles(String folder, String lastProcessedFile) {
        List <String> retval = new ArrayList<String>();
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
    
    private static boolean doUpgrade(Connection conn1, Connection conn2, Statement stmt) {
        boolean retval = true;
        writeHeader1("upgrading kfs");

        String lastProcessedFile = properties.getProperty("last-processed-file");

        List <String> folders = getFolders(lastProcessedFile);
        
        for (String folder : folders) {
            writeHeader2("processing folder " + folder);
            
            for (String fname : getFolderFiles(folder, lastProcessedFile)) {
                File f = getUpgradeFile(upgradeRoot + "/" + folder + "/" + fname);
                
                if (f.getName().endsWith(".sql")) {
                    if (!runSqlFile(conn1, stmt, f, ";")) {
                        retval = false;
                        writeProcessedFileInfo("[failure] " + f.getPath());
                    } else {
                        writeProcessedFileInfo("[success] " + f.getPath());
                    }
                } else {
                    if(!runLiquibase(conn2, f)) {
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
            
            if (!retval) {
                break;
            }
        }
        
        return retval;
    }
    
    private static boolean runLiquibase(Connection conn, File f) {
        boolean retval = true;
        writeHeader2("processing liquibase file " + f.getPath());
        PrintWriter pw = null;
        try {
            Liquibase liquibase = new Liquibase(f.getName(), new FileSystemFileOpener(f.getParentFile().getPath()), conn);
            liquibase.getDatabase().setAutoCommit(true);
            liquibase.reportStatus(true, null, pw = getOutputLogWriter());
            liquibase.update(null);
            retval = true;
        }
        
        catch (Exception ex) {
            retval= false;
            if (pw != null) {
                pw.close();
            }
            pw = null;
            writeOut(ex);
        }
        
        finally {
            if (pw != null) {
                pw.close();
            }
        }
        
        
        return retval;
    }

    private static void writeOut(Exception ex) {
        System.out.println();
        System.out.println(getTimeString() + ERROR);
        ex.printStackTrace(System.out);
        writeLog(ex);
    }
    
    private static void writeLog(Exception ex) {
        PrintWriter pw = null;
        
        try {
            pw = getOutputLogWriter();
            pw.println();
            pw.println(getTimeString() + ERROR);
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
		
        Properties props = new Properties();
        props.setProperty("user", properties.getProperty("database-user"));
        props.setProperty("password", properties.getProperty("database-password"));
        
        writeOut("Connecting to db " + properties.getProperty("database-name") + "...");
        writeOut("url=" + url);
			
        Class.forName(properties.getProperty("database-driver"));
		retval = DriverManager.getConnection(url, props);
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
            writeHeader2("dropping materialized view logs...");
            res = stmt.executeQuery("select LOG_OWNER || '.' || MASTER from SYS.user_mview_logs");
            
            List <String> logs = new ArrayList<String>();
            
            while (res.next()) {
                logs.add(res.getString(1));
            }

            for (String log : logs) {
                stmt.execute("drop materialized view log on " + log);
                writeOut("dropped materialized view log on " + log);
            }
            
            
            res.close();
            
            List <String> updates = new ArrayList<String>();
            
            writeHeader2("ensuring combination of (NM, NMSPC_CD) unique on KRIM_PERM_T and  KRIM_RSP_T...");

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
                writeOut("executing: " + sql);
                stmt.executeUpdate(sql);
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
    
    private static String getIndexTableName(String line) {
        String retval = null;
        int pos = line.indexOf(" ON ");

        if (pos > -1) {
            pos = line.indexOf(".", pos);
            int pos2 = line.indexOf("(", pos);
            
            if ((pos > -1) && (pos2 > -1) && (pos2 > pos)) {
                retval = line.substring(pos+1, pos2);
            }
        }
        
        return retval;
    }
    
    private static String getIndexName(String line) {
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

    private static List<String> getIndexColumnNames(String line) {
        List <String> retval = new ArrayList<String>();
        
        int pos = line.indexOf("(");
        int pos2 = line.indexOf(")");
        
        
        if ((pos > -1) && (pos2 > -1) && (pos2 > pos)) {
            StringTokenizer st = new StringTokenizer(line.substring(pos+1, pos2), ",");
            
            while (st.hasMoreTokens()) {
                retval.add(st.nextToken());
            }
        }
            
        return retval;
    }
    
    private static void createIndexes(Connection conn, Statement stmt) {
        LineNumberReader lnr = null;
        
        writeHeader2("creating KFS indexes ");

        try {
            lnr = new LineNumberReader(new FileReader(upgradeRoot + "/post-upgrade/sql/kfs-indexes.sql"));
            
            String line = null;
            
            while ((line = lnr.readLine()) != null) {
                String tableName = getIndexTableName(line);
                String indexName = getIndexName(line);
                
                if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(indexName)) {
                    boolean unique = line.contains(" UNIQUE ");
                    List <String> columnNames = getIndexColumnNames(line);
                    
                    if (!indexExists(conn, stmt, tableName, columnNames)) {
                        if (indexNameExists(conn, stmt, tableName, indexName)) {
                            indexName = getNextTableIndexName(conn, stmt, tableName);
                        }

                        StringBuilder sql = new StringBuilder(256);
                        
                        sql.append("CREATE ");
            
                        if (unique) {
                            sql.append ("UNIQUE ");
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
                        }
                        
                        catch (SQLException ex) {
                            writeOut("failed to create index: " + sql.toString());
                        }
                    }
                }
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
            
            catch (Exception ex) {};
        }
    }

    private static boolean indexExists(Connection conn, Statement stmt, String tableName, List <String> columnNames) throws Exception {
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
                
                List <String> columns = map.get(indexName);
                
                if (columns == null) {
                    map.put(indexName, columns = new ArrayList<String>());
                }
                
                columns.add(res.getString(2));
            }
            
            for (List <String> columns : map.values()) {
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
        }
        
        finally {
            closeDbObjects(null, null, res);
        }
        
        return retval;
    }
    
    private static boolean indexNameExists(Connection conn, Statement stmt, String tableName, String indexName) throws Exception {
        boolean retval = false;
        ResultSet res = null;
        
        try {
            res = stmt.executeQuery("select count(*) from user_indexes where table_owner = 'KULOWNER' and table_name = '" + tableName + "' and index_name = '" + indexName + "'");
            if (res.next()) {
                retval = (res.getInt(1) > 0);
            }
        }
        
        finally {
            closeDbObjects(null, null, res);
        }
        
        return retval;
    }
    
    private static String getNextTableIndexName(Connection conn, Statement stmt, String tableName) throws Exception {
        String retval = null;

        int maxIndex = -1;
        ResultSet res = null;
        
        try {
            res = stmt.executeQuery("select index_name from user_indexes where table_owner = 'KULOWNER' and table_name = '" + tableName + "'");
            while (res.next()) {
                String indexName = res.getString(1);
                int pos = indexName.lastIndexOf("_");

                if (pos > -1) {
                    if (indexName.substring(pos+1).startsWith("TI")) {
                        try {
                            int i = Integer.parseInt(indexName.substring(pos+3));
                            if (i > maxIndex) {
                                maxIndex = i;
                                
                            }
                        }
                        
                        catch (NumberFormatException ex) {};
                    }
                }
            }
            
            retval = (tableName + "I" + (maxIndex+1));
        }
        
        finally {
            closeDbObjects(null, null, res);
        }
        
        return retval;
    }

    private static String getSynonymName(String line) {
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
    
    private static boolean synonymExists(Connection conn, Statement stmt, String synonymName) throws Exception {
        boolean retval = false;
        ResultSet res = null;
        
        try {
            res = stmt.executeQuery("select count(*) from all_synonyms where owner = 'PUBLIC' and synonym_name = '" + synonymName + "'");
            if (res.next()) {
                retval = (res.getInt(1) > 0);
            }
        }
        
        finally {
            closeDbObjects(null, null, res);
        }
        
        return retval;
    }
    
    
    private static void createPublicSynonyms(Connection conn, Statement stmt) {
        LineNumberReader lnr = null;
        
        writeHeader2("creating KFS public synonyms ");

        try {
            lnr = new LineNumberReader(new FileReader(upgradeRoot + "/post-upgrade/sql/kfs-public-synonyms.sql"));
            
            String line = null;
            
            while ((line = lnr.readLine()) != null) {
                String synonymName = getSynonymName(line);
                
                if (!synonymExists(conn, stmt, synonymName)) {
                    try {
                        stmt.execute(line);
                    }
                    
                    catch (SQLException ex) {
                        writeOut("faled to create public synonym: " + line);
                    }
                }
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
            
            catch (Exception ex) {};
        }
    }
}
