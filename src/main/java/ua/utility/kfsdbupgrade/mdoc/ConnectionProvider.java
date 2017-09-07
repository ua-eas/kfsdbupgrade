package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import javax.inject.Provider;

public final class ConnectionProvider implements Provider<Connection> {

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
      Class.forName(driver);
      Connection conn = DriverManager.getConnection(url, username, password);
      conn.setAutoCommit(false);
      return conn;
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
  }

}
