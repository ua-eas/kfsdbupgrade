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

public final class OracleCpusProvider implements Provider<Optional<Integer>> {

  private static final Logger LOGGER = getLogger(OracleCpusProvider.class);

  public OracleCpusProvider(Connection conn) {
    this.conn = checkNotNull(conn);
  }

  private final Connection conn;

  public Optional<Integer> get() {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      // create a jdbc statement
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
    }
  }

}
