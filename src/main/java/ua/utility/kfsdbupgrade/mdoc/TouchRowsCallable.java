package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocSelector.asInClause;
import static ua.utility.kfsdbupgrade.mdoc.Show.show;
import static ua.utility.kfsdbupgrade.mdoc.Stopwatches.synchronizedStart;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Provider;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class TouchRowsCallable implements Callable<Long> {

  private final Provider<Connection> provider;
  private final int batchSize;
  private final ImmutableList<String> rowIds;
  private final DataMetrics metrics;
  private final Stopwatch sw;
  private final Optional<String> schema;
  private final String table;
  private final String field;
  private final Optional<Integer> max;
  private final Optional<Integer> show;

  @Override
  public Long call() {
    synchronizedStart(sw);
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      conn = provider.get();
      stmt = conn.createStatement();
      for (List<String> partition : partition(rowIds, batchSize)) {
        String from = schema.isPresent() ? schema.get() + "." + table : table;
        String sql = format("SELECT %s FROM %s WHERE ROWID IN (" + asInClause(partition) + ")", field, from);
        rs = stmt.executeQuery(sql);
        Stopwatch timer = createStarted();
        while (rs.next()) {
          int length = rs.getString(1).length();
          synchronized (metrics) {
            timer = metrics.increment(1, length, timer);
            if (show.isPresent() && metrics.getCount() % show.get() == 0) {
              show(metrics, sw, "");
            }
          }
        }
      }
      synchronized (metrics) {
        show(metrics, sw, "done");
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }
    return sw.elapsed(MILLISECONDS);
  }

  private TouchRowsCallable(Builder builder) {
    this.provider = builder.provider;
    this.batchSize = builder.batchSize;
    this.rowIds = builder.rowIds;
    this.metrics = builder.metrics;
    this.sw = builder.sw;
    this.schema = builder.schema;
    this.table = builder.table;
    this.max = builder.max;
    this.show = builder.show;
    this.field = builder.field;
  }

  public static class Builder {

    private Provider<Connection> provider;
    private int batchSize;
    private ImmutableList<String> rowIds;
    private DataMetrics metrics;
    private Stopwatch sw;
    private Optional<String> schema;
    private String table;
    private String field;
    private Optional<Integer> max;
    private Optional<Integer> show;

    public Builder withProvider(Provider<Connection> provider) {
      this.provider = provider;
      return this;
    }

    public Builder withBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder withRowIds(ImmutableList<String> rowIds) {
      this.rowIds = rowIds;
      return this;
    }

    public Builder withMetrics(DataMetrics metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder withSw(Stopwatch sw) {
      this.sw = sw;
      return this;
    }

    public Builder withSchema(Optional<String> schema) {
      this.schema = schema;
      return this;
    }

    public Builder withField(String field) {
      this.field = field;
      return this;
    }

    public Builder withTable(String table) {
      this.table = table;
      return this;
    }

    public Builder withMax(Optional<Integer> max) {
      this.max = max;
      return this;
    }

    public Builder withShow(Optional<Integer> show) {
      this.show = show;
      return this;
    }

    public TouchRowsCallable build() {
      return validate(new TouchRowsCallable(this));
    }

    private static TouchRowsCallable validate(TouchRowsCallable instance) {
      checkNotNull(instance.provider, "provider may not be null");
      checkNotNull(instance.batchSize > 0, "batchSize should be set");
      checkNotNull(instance.rowIds, "rowIds may not be blank");
      checkNotNull(instance.metrics, "metrics may not be null");
      checkNotNull(instance.sw, "sw may not be null");
      checkNotNull(instance.schema, "schema may not be blank");
      checkNotNull(instance.table, "table may not be blank");
      checkNotNull(instance.max, "max may not be null");
      checkNotNull(instance.show, "show may not be null");
      return instance;
    }
  }

}
