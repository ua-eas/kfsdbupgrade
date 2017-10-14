package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.lang.String.format;
import static java.sql.DriverManager.getConnection;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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

  public ConnectionProvider(Properties props, boolean autoCommit) {
    this.props = checkNotNull(props);
    this.autoCommit = autoCommit;
  }

  private final Properties props;
  private final boolean autoCommit;

  public Connection get() {
    Connection connection = null;
    try {
      String username = checkedValue(props, "database-user");
      String password = checkedValue(props, "database-password");
      String url;
      if (!props.containsKey("db.url")) {
        String name = checkedValue(props, "db.name");
        String fragment = checkedValue(props, "db.fragment");
        String port = props.getProperty("db.port", "1521");
        String sid = props.getProperty("db.sid", name);
        String formatted = format("jdbc:oracle:thin:@%s.%s:%s:%s", name.toLowerCase(), fragment, port, sid.toUpperCase());
        url = formatted;
      } else {
        url = props.getProperty("db.url");
      }
      info("%s as '%s'", url, username);
      Class.forName(checkedValue(props, "database-driver"));
      Stopwatch sw = createStarted();
      connection = getConnection(url, username, password);
      connection.setAutoCommit(autoCommit);
      info("%s as '%s' [%s]", url, username, getTime(sw));
      return connection;
    } catch (Throwable e) {
      closeQuietly(connection);
      throw new IllegalStateException(e);
    }
  }

  private String checkedValue(Properties props, String key) {
    String value = props.getProperty(key);
    checkArgument(isNotBlank(value), "%s cannot be blank", key);
    return value;
  }

  private void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

}
