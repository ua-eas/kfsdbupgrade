package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static java.lang.Integer.parseInt;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocSelector.asInClause;
import static ua.utility.kfsdbupgrade.mdoc.Stopwatches.synchronizedStart;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class TouchRowsCallable implements Callable<Long> {

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
  private final int show = parseInt(System.getProperty("mdoc.show", "1000"));

  @Override
  public Long call() {
    synchronizedStart(sw);
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      RowIdConverter converter = new RowIdConverter();
      for (List<RowId> partition : partition(rows, batchSize)) {
        List<String> rowIds = transform(partition, converter.reverse());
        String sql = String.format("SELECT %s FROM KRNS_MAINT_DOC_T WHERE ROWID IN (" + asInClause(rowIds) + ")", field);
        rs = stmt.executeQuery(sql);
        Stopwatch timer = createStarted();
        while (rs.next()) {
          int length = rs.getString(1).length();
          synchronized (metrics) {
            timer = metrics.increment(1, length, timer);
            if (metrics.getCount() % show == 0) {
              new Show(metrics, sw, "").get();
            }
          }
        }
      }
      synchronized (metrics) {
        new Show(metrics, sw, "done").get();
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(stmt);
      closeQuietly(conn);
    }
    return 0L;
  }

  public Connection getConn() {
    return conn;
  }

  public int getBatchSize() {
    return batchSize;
  }

}
