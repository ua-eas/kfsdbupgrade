package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;
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

public final class TouchRowsCallable implements Callable<Long> {

  private static final Logger LOGGER = getLogger(TouchRowsCallable.class);

  public TouchRowsCallable(Connection conn, int batchSize, Iterable<RowId> rows, DataMetrics metrics, Stopwatch sw, MaintDocField field) {
    this.conn = checkNotNull(conn);
    this.batchSize = batchSize;
    this.rows = copyOf(rows);
    this.metrics = metrics;
    this.sw = sw;
    this.field = field;
  }

  private final Connection conn;
  private final int batchSize;
  private final ImmutableList<RowId> rows;
  private final DataMetrics metrics;
  private final Stopwatch sw;
  private final MaintDocField field;

  @Override
  public Long call() {
    synchronizedStart(sw);
    Stopwatch overall = createStarted();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      RowIdConverter converter = new RowIdConverter();
      DataMetrics current = new DataMetrics();
      Stopwatch timer = createStarted();
      for (List<RowId> partition : partition(rows, batchSize)) {
        List<String> rowIds = transform(partition, converter.reverse());
        String sql = String.format("SELECT %s FROM KRNS_MAINT_DOC_T WHERE ROWID IN (" + asInClause(rowIds, true) + ")", field);
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
          int length = rs.getString(1).length();
          long elapsed = timer.elapsed(MICROSECONDS);
          current.increment(1, length, elapsed);
          synchronized (metrics) {
            metrics.increment(1, length, elapsed);
            if (current.getCount().getValue() % 10 == 0) {
              new Show(metrics, sw, current, timer, "").get();
              timer = createStarted();
            }
          }
        }
      }
      synchronized (metrics) {
        new Show(metrics, sw, current, timer, "done").get();
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
