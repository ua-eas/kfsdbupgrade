package ua.utility.kfsdbupgrade;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Program to execute conversion of individual documents. This is intended to
 * convert documents for ad-hoc testing in between full database upgrade and
 * refresh runs.
 */
public class SingleXmlUpgrader {

	private static final Logger LOGGER = Logger.getLogger(SingleXmlUpgrader.class.getName());
	private static final String DB_USER = "kulowner";
	private static final String KFS3_DB_CONNECTION_STRING = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=uaz-dv-scan-01.mosaic.arizona.edu)(PORT=1521)))(CONNECT_DATA=(SERVER=dedicated)(SERVICE_NAME=kfdvu.mosaic.arizona.edu)))";
	private static final String KFS6_DB_CONNECTION_STRING = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=uaz-dv-scan-01.mosaic.arizona.edu)(PORT=1521)))(CONNECT_DATA=(SERVER=dedicated)(SERVICE_NAME=kfdev.mosaic.arizona.edu)))";
	private static final String GET_DOC_QUERY = "SELECT DOC_CNTNT FROM krns_maint_doc_t where DOC_HDR_ID = ?";
	private static final String UPDATE_DOC_BATCH = "DECLARE str varchar2(32767); BEGIN str := ?; update KRNS_MAINT_DOC_T set doc_cntnt = str where doc_hdr_id = ?; END;";

	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args.length > 3) {
			LOGGER.fatal("Usage: SingleXmlUpgrader encryptionKey documentNumber fileDirectory(Optional).\n  Note: if fileDirectory is not specified, converted files will not be written to disk and will be silently processed.");
		}
		LOGGER.setLevel(Level.TRACE);
		File baseDir = null;
		if (args.length == 3) {
			baseDir = new File(args[2]);
			if (!(baseDir.exists() && baseDir.isDirectory() && baseDir.canWrite())) {
				throw new RuntimeException("Unable to write to specified fileDirectory: " + args[2]);
			}
		}
		boolean writing = baseDir != null;

		// query KFS3 DB for ciphertext
		String docNumber = args[1];
		Connection kfs3Conn = getConnection(KFS3_DB_CONNECTION_STRING);
		PreparedStatement kfs3Stmt = kfs3Conn.prepareStatement(GET_DOC_QUERY);
		kfs3Stmt.setString(1, docNumber);
		ResultSet rs = kfs3Stmt.executeQuery();

		String kfs3Encrypted = null;
		while (rs.next()) {
			kfs3Encrypted = rs.getString("DOC_CNTNT");
		}
		if (kfs3Encrypted == null) {
			throw new RuntimeException("Document number " + docNumber + " not found in KFS3 database.");
		}
		close(rs);
		close(kfs3Stmt);
		close(kfs3Conn);

		if (writing) {
			FileUtils.writeStringToFile(new File(baseDir, docNumber + "-KFS3-encrypted"), kfs3Encrypted);
		}

		// decrypt
		EncryptionService encryptService = new EncryptionService(args[0]);
		String kfs3Xml = encryptService.decrypt(kfs3Encrypted);

		if (writing) {
			FileUtils.writeStringToFile(new File(baseDir, docNumber + "-KFS3.xml"), kfs3Xml);
		}

		// upgrade file
		File f = new File("src/main/resources/MaintainableXMLUpgradeRules.xml");
		MaintainableXMLConversionServiceImpl maintainableXMLConversionServiceImpl = new MaintainableXMLConversionServiceImpl(
				f, Level.TRACE);
		String kfs6Xml = maintainableXMLConversionServiceImpl.transformMaintainableXML(kfs3Xml, docNumber);

		if (writing) {
			FileUtils.writeStringToFile(new File(baseDir, docNumber + "-KFS6.xml"), kfs6Xml);
		}

		// encrypt file
		String kfs6Encrypted = encryptService.encrypt(kfs6Xml);
		if (writing) {
			FileUtils.writeStringToFile(new File(baseDir, docNumber + "-KFS6-encrypted"), kfs6Encrypted);
		}

		// upgrade document in target database
		Connection kfs6Connection = getConnection(KFS6_DB_CONNECTION_STRING);
		PreparedStatement kfs6Stmt = kfs6Connection.prepareStatement(UPDATE_DOC_BATCH);
		kfs6Stmt.setString(1, kfs6Encrypted);
		kfs6Stmt.setString(2, docNumber);
		kfs6Stmt.addBatch();
		kfs6Stmt.executeBatch();
		kfs6Connection.commit();

		close(kfs6Stmt);
		close(kfs6Connection);

	}

	/**
	 * Boilerplate to eat the potential {@link SQLException} when closing a
	 * {@link Connection}.
	 * 
	 * @param conn
	 */
	private static void close(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			LOGGER.warn("Exception encountered when closing database connection. Continuing.", e);
		}
	}

	/**
	 * Boilerplate to eat the potential {@link SQLException} when closing a
	 * {@link PreparedStatement}.
	 * 
	 * @param stmt
	 */
	private static void close(PreparedStatement stmt) {
		try {
			stmt.close();
		} catch (SQLException e) {
			LOGGER.warn("Exception encountered when closing prepared statement. Continuing.", e);
		}
	}

	/**
	 * Boilerplate to eat the potential {@link SQLException} when closing a
	 * {@link ResultSet}.
	 * 
	 * @param rs
	 */
	private static void close(ResultSet rs) {
		try {
			rs.close();
		} catch (SQLException e) {
			LOGGER.warn("Exception encountered when closing result set. Continuing.", e);
		}
	}

	/**
	 * @param connectionString
	 *            {@link String} of an Oracle JDBC URL to connect to
	 * @return {@link Connection} to the target database
	 * @throws ClassNotFoundException
	 *             if the Oracle JDBC driver can not be loaded
	 * @throws SQLException
	 *             Any {@link SQLException}s encountered will be rethrown
	 */
	private static Connection getConnection(String connectionString) throws ClassNotFoundException, SQLException {
		Connection retval = null;
		String url = connectionString;

		Properties props = new Properties();
		props.setProperty("user", DB_USER);
		props.setProperty("password", DB_USER);

		LOGGER.info("Connecting to database with connection definition: " + connectionString + "...");
		LOGGER.info("url=" + url);

		Class.forName("oracle.jdbc.driver.OracleDriver");
		retval = DriverManager.getConnection(url, props);
		retval.setReadOnly(false);
		retval.setAutoCommit(false);

		LOGGER.info("Connected to database with connection definition: " + connectionString);

		return retval;
	}

}
