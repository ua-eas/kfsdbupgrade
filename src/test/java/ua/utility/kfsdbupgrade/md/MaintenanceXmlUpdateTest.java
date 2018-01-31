package ua.utility.kfsdbupgrade.md;

import java.io.File;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import ua.utility.kfsdbupgrade.EncryptionService;



public class MaintenanceXmlUpdateTest {
    private static final Logger LOG = Logger.getLogger(MaintenanceXmlUpdateTest.class);

    // Must be provided
    private static final String KFS_3_0_PASSWORD = "kulowner";
    private static final String KFS7_PASSWORD = "kulowner";
    private static final String RICE_2_5_PASSWORD = "c75yvsjxua";
    private static final String ENCRYPTION_KEY = "7IC64w6kAAAA";
    private static final String OUTPUT_XML_PATH = "/home/sskinner/tmp/output-doc-xml";

    // KFS v3.0
    private static final String KFS_3_0_USERNAME = "kulowner";
    private static final String KFS_3_0_CONN_STRING = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=uaz-dv-scan-01.mosaic.arizona.edu)(PORT=1521)))(CONNECT_DATA=(SERVER=dedicated)(SERVICE_NAME=kfdvu.mosaic.arizona.edu)))";

    // KFS v7
    private static final String KFS7_USERNAME = "kulowner";
    private static final String KFS7_CONN_STRING = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=kfs7dev.cryfdp9i1uh4.us-west-2.rds.amazonaws.com)(PORT=1521)))(CONNECT_DATA=(SERVER=dedicated)(SERVICE_NAME=kfs7dev)))";

    // Rice v2.5
    private static final String RICE_2_5_USERNAME = "rice";
    private static final String RICE_2_5_CONN_STRING = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=kfs7dev.cryfdp9i1uh4.us-west-2.rds.amazonaws.com)(PORT=1521)))(CONNECT_DATA=(SERVER=dedicated)(SERVICE_NAME=kfs7dev)))";

    private static final String GET_DOC_QUERY = "SELECT DOC_CNTNT FROM krns_maint_doc_t where DOC_HDR_ID = ?";

    private static final Set<String> docIdsToPull;
    static {
        docIdsToPull = new HashSet<>();
        docIdsToPull.add("712293");
        docIdsToPull.add("712095");
        docIdsToPull.add("772313");
        docIdsToPull.add("794038");
        docIdsToPull.add("794041");
    }



    @Ignore//Convenience, not really a test
    @Test
    public void testPullingXml() throws Exception {
        EncryptionService encryptService = new EncryptionService(ENCRYPTION_KEY);

        for (String docNumber : docIdsToPull) {
            // Pull KFS v3 XML
            Connection kfs3Conn = getKfs3Connection();
            String kfs3xml;
            try {
                kfs3xml = pullXml(docNumber, kfs3Conn, encryptService);
            } finally {
                close(kfs3Conn);
            }
            if (StringUtils.isNotBlank(kfs3xml)) {
                FileUtils.writeStringToFile(new File(OUTPUT_XML_PATH, docNumber + "-KFS3.xml"), kfs3xml);
            }

            // Pull KFS v7 XML
            Connection kfs7Conn = getKfs7Connection();
            String kfs7xml;
            try {
                kfs7xml = pullXml(docNumber, kfs7Conn, encryptService);
            } finally {
                close(kfs7Conn);
            }
            if (StringUtils.isNotBlank(kfs7xml)) {
                FileUtils.writeStringToFile(new File(OUTPUT_XML_PATH, docNumber + "-KFS7.xml"), kfs7xml);
            }

            // Pull Rice 2.5 XML
            Connection rice25Conn = getRice25Connection();
            String rice25xml;
            try {
                rice25xml = pullXml(docNumber, rice25Conn, encryptService);
            } finally {
                close(rice25Conn);
            }
            if (StringUtils.isNotBlank(rice25xml)) {
                FileUtils.writeStringToFile(new File(OUTPUT_XML_PATH, docNumber + "-RICE25.xml"), rice25xml);
            }
        }

    }


    private String pullXml(String docNumber, Connection connection, EncryptionService encryptionService) throws SQLException, GeneralSecurityException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String decryptedXml = null;
        try {
            statement = connection.prepareStatement(GET_DOC_QUERY);
            statement.setString(1, docNumber);
            resultSet = statement.executeQuery();
            resultSet.next();
            String encryptedXml = resultSet.getString("DOC_CNTNT");
            decryptedXml = encryptionService.decrypt(encryptedXml);
        } catch (java.sql.SQLException e) {
            System.out.printf("Couldn't execute SQL: %s\n", e.getMessage());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            close(resultSet, statement);
        }

        return decryptedXml;
    }


    private Connection getKfs3Connection() throws SQLException, ClassNotFoundException {
        return getConnection(KFS_3_0_USERNAME, KFS_3_0_PASSWORD, KFS_3_0_CONN_STRING);
    }


    private Connection getKfs7Connection() throws SQLException, ClassNotFoundException {
        return getConnection(KFS7_USERNAME, KFS7_PASSWORD, KFS7_CONN_STRING);
    }


    private Connection getRice25Connection() throws SQLException, ClassNotFoundException {
        return getConnection(RICE_2_5_USERNAME, RICE_2_5_PASSWORD, RICE_2_5_CONN_STRING);
    }


    private static Connection getConnection(String user, String password, String connectionString) throws ClassNotFoundException, SQLException {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);

        LOG.info("Connecting to database with connection definition: " + connectionString + "...");
        LOG.info("url=" + connectionString);


        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection connection = DriverManager.getConnection(connectionString, props);
        connection.setReadOnly(false);
        connection.setAutoCommit(false);

        LOG.info("Connection created for: " + connectionString);
        LOG.info("As schema user: " + user);

        return connection;
    }


    private void close(AutoCloseable...closeables) {
        try {
            for (AutoCloseable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
