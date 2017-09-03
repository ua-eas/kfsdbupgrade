package ua.utility.kfsdbupgrade.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.lang.String.format;
import static java.sql.DriverManager.getConnection;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.sql.Connection;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;

public final class ConnectionProvider implements Provider<Connection> {

  private static final Logger LOGGER = getLogger(ConnectionProvider.class);

  public ConnectionProvider(Properties properties) {
    this.properties = checkNotNull(properties);
  }

  private final Properties properties;

  @Override
  public Connection get() {
    Connection connection = null;
    try {
      String url = properties.getProperty("database-url");
      Properties props = new Properties();
      props.setProperty("user", properties.getProperty("database-user"));
      props.setProperty("password", properties.getProperty("database-password"));
      LOGGER.info("connecting to -> " + url);
      Class.forName(properties.getProperty("database-driver"));
      Stopwatch sw = createStarted();
      connection = getConnection(url, props);
      connection.setReadOnly(false);
      connection.setAutoCommit(false);
      LOGGER.info(format("connected to --> %s [%s]", connection.getMetaData().getURL(), getTime(sw)));
      return connection;
    } catch (Throwable e) {
      closeQuietly(connection);
      throw new IllegalStateException(e);
    }
  }

}
