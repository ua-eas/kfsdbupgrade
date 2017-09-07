package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;

public final class ConnectionProvider implements Provider<Connection> {

  private static final Logger LOGGER = Logger.getLogger(ConnectionProvider.class);

  public ConnectionProvider(Properties properties) {
    this.properties = checkNotNull(properties);
  }

  private final Properties properties;

  public Connection get() {
    try {
      String username = properties.getProperty("database-user");
      String password = properties.getProperty("database-password");
      String driver = properties.getProperty("database-driver");
      String url = properties.getProperty("database-url");
      LOGGER.info("connecting to " + url + " as '" + username + "'");
      Stopwatch sw = createStarted();
      Class.forName(driver);
      Connection conn = DriverManager.getConnection(url, username, password);
      conn.setAutoCommit(false);
      LOGGER.info("connected to " + url + " as '" + username + "' [" + getTime(sw) + "]");
      return conn;
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
  }

}
