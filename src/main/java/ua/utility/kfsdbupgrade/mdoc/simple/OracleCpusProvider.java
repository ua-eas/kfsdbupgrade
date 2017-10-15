package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;

import ua.utility.kfsdbupgrade.mdoc.Providers;

public final class OracleCpusProvider implements Provider<Optional<Integer>> {

  private static final Logger LOGGER = getLogger(OracleCpusProvider.class);

  public OracleCpusProvider(Connection conn) {
    this(Providers.of(conn), false);
  }

  public OracleCpusProvider(Provider<Connection> provider) {
    this(provider, true);
  }

  private OracleCpusProvider(Provider<Connection> provider, boolean closeConnection) {
    this.provider = checkNotNull(provider);
    this.closeConnection = closeConnection;
  }

  private final Provider<Connection> provider;
  private final boolean closeConnection;

  public Optional<Integer> get() {
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      // establish a connection to the db and create a statement
      conn = provider.get();
      stmt = conn.createStatement();
      // pull the cpu count from the V$OSSTAT table
      rs = stmt.executeQuery("SELECT VALUE FROM V$OSSTAT WHERE STAT_NAME ='NUM_CPUS'");
      // move to the first and only row
      rs.next();
      // extract the cpu count
      int cpus = rs.getInt(1);
      // return what we've found
      return of(cpus);
    } catch (Throwable e) {
      // if anything goes wrong at any point, log it, and return absent
      LOGGER.error("unexpected error determining Oracle CPU count", e);
      return absent();
    } finally {
      // cleanup
      closeQuietly(rs);
      closeQuietly(stmt);
      if (closeConnection) {
        closeQuietly(conn);
      }
    }
  }

}
