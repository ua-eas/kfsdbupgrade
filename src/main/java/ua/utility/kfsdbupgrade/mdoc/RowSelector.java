package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocSelector.asInClause;
import static ua.utility.kfsdbupgrade.mdoc.Stopwatches.synchronizedStart;
import static ua.utility.kfsdbupgrade.mdoc.Validation.checkNoBlanks;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class RowSelector<T> implements Provider<ImmutableList<T>> {

  private final Provider<Connection> provider;
  private final int batchSize;
  private final ImmutableList<String> rowIds;
  private final DataMetrics metrics;
  private final Stopwatch timer;
  private final Optional<String> schema;
  private final String table;
  private final ImmutableList<String> fields;
  private final Function<ResultSet, T> function;
  private final Function<T, Long> weigher;
  private final Optional<Integer> show;
  private final Optional<Integer> max;

  @Override
  public ImmutableList<T> get() {
    synchronizedStart(timer);
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      conn = provider.get();
      stmt = conn.createStatement();
      String select = Joiner.on(',').join(fields);
      String from = schema.isPresent() ? schema.get() + "." + table : table;
      if (rowIds.size() > 0) {
        return doRowIds(stmt, select, from);
      } else {
        return doSelect(stmt, select, from);
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      synchronized (metrics) {
        new Show(metrics, timer, "done").get();
      }
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }
  }

  private ImmutableList<T> doSelect(Statement stmt, String select, String from) throws IOException {
    ResultSet rs = null;
    try {
      String where = max.isPresent() ? "ROWNUM <= " + max.get() : "";
      String sql = format("SELECT %s FROM %s %s", select, from, where).trim();
      rs = stmt.executeQuery(sql);
      return doResultSet(rs);
    } catch (Throwable e) {
      throw new IOException(e);
    } finally {
      closeQuietly(rs);
    }
  }

  private ImmutableList<T> doRowIds(Statement stmt, String select, String from) throws IOException {
    List<T> list = newArrayList();
    ResultSet rs = null;
    try {
      for (List<String> partition : partition(rowIds, batchSize)) {
        String sql = format("SELECT %s FROM %s WHERE ROWID IN (%s)", select, from, asInClause(partition));
        rs = stmt.executeQuery(sql);
        List<T> batch = doResultSet(rs);
        list.addAll(batch);
      }
    } catch (Throwable e) {
      throw new IOException(e);
    } finally {
      closeQuietly(rs);
    }
    return newList(list);
  }

  private ImmutableList<T> doResultSet(ResultSet rs) throws SQLException {
    Stopwatch sw = createStarted();
    List<T> list = newArrayList();
    while (rs.next()) {
      T instance = function.apply(rs);
      long weight = weigher.apply(instance);
      sw = increment(metrics, weight, sw);
      list.add(instance);
    }
    return newList(list);
  }

  private Stopwatch increment(DataMetrics metrics, long weight, Stopwatch sw) {
    synchronized (metrics) {
      metrics.increment(1, weight, sw);
      if (show.isPresent() && metrics.getCount() % show.get() == 0) {
        new Show(metrics, timer, "").get();
      }
    }
    return createStarted();
  }

  private RowSelector(Builder<T> builder) {
    this.provider = builder.provider;
    this.batchSize = builder.batchSize;
    this.rowIds = copyOf(builder.rowIds);
    this.metrics = builder.metrics;
    this.timer = builder.timer;
    this.schema = builder.schema;
    this.table = builder.table;
    this.show = builder.show;
    this.fields = copyOf(builder.fields);
    this.function = builder.function;
    this.weigher = builder.weigher;
    this.max = builder.max;
  }

  public static class Builder<T> {

    private Provider<Connection> provider;
    private int batchSize;
    private List<String> rowIds;
    private DataMetrics metrics;
    private Stopwatch timer;
    private Optional<String> schema = absent();
    private String table;
    private List<String> fields;
    private Optional<Integer> max = absent();
    private Optional<Integer> show = absent();
    private Function<ResultSet, T> function;
    private Function<T, Long> weigher;

    public Builder<T> withMax(Optional<Integer> max) {
      this.max = max;
      return this;
    }

    public Builder<T> withMax(int max) {
      return withMax(of(max));
    }

    public Builder<T> withWeigher(Function<T, Long> weigher) {
      this.weigher = weigher;
      return this;
    }

    public Builder<T> withFunction(Function<ResultSet, T> function) {
      this.function = function;
      return this;
    }

    public Builder<T> withProvider(Provider<Connection> provider) {
      this.provider = provider;
      return this;
    }

    public Builder<T> withBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder<T> withRowIds(List<String> rowIds) {
      this.rowIds = rowIds;
      return this;
    }

    public Builder<T> withMetrics(DataMetrics metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder<T> withTimer(Stopwatch timer) {
      this.timer = timer;
      return this;
    }

    public Builder<T> withSchema(Optional<String> schema) {
      this.schema = schema;
      return this;
    }

    public Builder<T> withField(List<String> fields) {
      this.fields = fields;
      return this;
    }

    public Builder<T> withTable(String table) {
      this.table = table;
      return this;
    }

    public Builder<T> withShow(Optional<Integer> show) {
      this.show = show;
      return this;
    }

    public Builder<T> withShow(int show) {
      return withShow(of(show));
    }

    public RowSelector<T> build() {
      return checkNoBlanks(new RowSelector<T>(this));
    }

  }

}
