package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static com.google.common.primitives.Ints.checkedCast;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.base.Lists.transform;
import static ua.utility.kfsdbupgrade.base.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocSelector.asInClause;
import static ua.utility.kfsdbupgrade.mdoc.Stopwatches.synchronizedStart;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class ReadClobsCallable implements Callable<Long> {

  private static final Logger LOGGER = getLogger(ReadClobsCallable.class);

  public ReadClobsCallable(Connection conn, int batchSize, Iterable<RowId> rows, Counter counter, Stopwatch sw) {
    this.conn = checkNotNull(conn);
    this.batchSize = batchSize;
    this.rows = copyOf(rows);
    this.counter = counter;
    this.sw = sw;
  }

  private final Connection conn;
  private final int batchSize;
  private final ImmutableList<RowId> rows;
  private final Counter counter;
  private final Stopwatch sw;

  @Override
  public Long call() {
    synchronizedStart(sw);
    Stopwatch overall = createStarted();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      RowIdConverter converter = RowIdConverter.getInstance();
      for (List<RowId> partition : partition(rows, batchSize)) {
        List<String> rowIds = transform(partition, converter.reverse());
        String sql = "SELECT DOC_CONTNT FROM KRNS_MAINT_DOC_T WHERE ROWID IN (" + asInClause(rowIds, true) + ")";
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
          rs.getString(1);
          synchronized (counter) {
            int count = checkedCast(counter.increment());
            if (count % 1000 == 0) {
              info(LOGGER, "%s [%s]", getCount(count), getTime(sw));
            }
          }
        }
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(stmt);
      closeQuietly(conn);
    }
    return overall.elapsed(MILLISECONDS);
  }

  public Connection getConn() {
    return conn;
  }

  public int getBatchSize() {
    return batchSize;
  }

}
