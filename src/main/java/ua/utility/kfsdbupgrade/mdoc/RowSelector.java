package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Stopwatch.createUnstarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocSelector.asInClause;
import static ua.utility.kfsdbupgrade.mdoc.Show.show;
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
  private final DataMetrics overall;
  private final DataMetrics current;
  private final Stopwatch timer;
  private final Stopwatch last;
  private final Optional<String> schema;
  private final String table;
  private final ImmutableList<String> fields;
  private final Function<ResultSet, T> function;
  private final Function<T, Long> weigher;
  private final Optional<Integer> show;
  private final Optional<Integer> max;
  private final boolean discard;
  private final boolean closeConnection;

  @Override
  public ImmutableList<T> get() {
    synchronizedStart(timer, last);
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
      show(overall, current, timer, last, "done");
      closeQuietly(rs);
      closeQuietly(stmt);
      if (closeConnection) {
        closeQuietly(conn);
      }
    }
  }

  private ImmutableList<T> doSelect(Statement stmt, String select, String from) throws IOException {
    ResultSet rs = null;
    try {
      String where = max.isPresent() ? "WHERE ROWNUM <= " + max.get() : "";
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
      if (!discard) {
        list.add(instance);
      }
      sw = increment(weight, sw);
    }
    return newList(list);
  }

  private Stopwatch increment(long weight, Stopwatch sw) {
    long micros = sw.elapsed(MICROSECONDS);
    synchronized (overall) {
      synchronized (current) {
        this.overall.increment(1, weight, micros);
        this.current.increment(1, weight, micros);
        if (show.isPresent() && overall.getCount() % show.get() == 0) {
          show(overall, current, timer, last);
          this.current.reset();
          this.last.reset().start();
        }
      }
    }
    return createStarted();
  }

  private RowSelector(Builder<T> builder) {
    this.provider = builder.provider;
    this.batchSize = builder.batchSize;
    this.rowIds = copyOf(builder.rowIds);
    this.overall = builder.overall;
    this.timer = builder.timer;
    this.schema = builder.schema;
    this.table = builder.table;
    this.show = builder.show;
    this.fields = copyOf(builder.fields);
    this.function = builder.function;
    this.weigher = builder.weigher;
    this.max = builder.max;
    this.discard = builder.discard;
    this.current = builder.current;
    this.last = builder.last;
    this.closeConnection = builder.closeConnection;
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  public static class Builder<T> {

    private Provider<Connection> provider;
    private int batchSize = 75;
    private List<String> rowIds = newArrayList();
    private DataMetrics overall = new DataMetrics();
    private DataMetrics current = new DataMetrics();
    private Stopwatch timer = createUnstarted();
    private Stopwatch last = createUnstarted();
    private Optional<String> schema = absent();
    private String table;
    private List<String> fields = newArrayList("ROWID");
    private Optional<Integer> max = absent();
    private Optional<Integer> show = absent();
    private Function<ResultSet, T> function;
    private Function<T, Long> weigher;
    private boolean discard;
    private boolean closeConnection;

    public Builder<T> withCloseConnection(boolean closeConnection) {
      this.closeConnection = closeConnection;
      return this;
    }

    public Builder<T> withLast(Stopwatch last) {
      this.last = last;
      return this;
    }

    public Builder<T> withCurrent(DataMetrics current) {
      this.current = current;
      return this;
    }

    public Builder<T> withDiscard(boolean discard) {
      this.discard = discard;
      return this;
    }

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

    public Builder<T> withConnection(Connection connection, boolean closeConnection) {
      withProvider(Providers.of(connection));
      return withCloseConnection(closeConnection);
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

    public Builder<T> withOverall(DataMetrics overall) {
      this.overall = overall;
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

    public Builder<T> withField(String field) {
      return withFields(asList(field));
    }

    public Builder<T> withFields(List<String> fields) {
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
      return validate(new RowSelector<T>(this));
    }

    private static <T> RowSelector<T> validate(RowSelector<T> instance) {
      checkNoBlanks(instance);
      if (instance.rowIds.size() > 0) {
        checkArgument(!instance.max.isPresent(), "max is ignored when row ids are supplied");
      }
      return instance;
    }

  }

  public Provider<Connection> getProvider() {
    return provider;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public ImmutableList<String> getRowIds() {
    return rowIds;
  }

  public DataMetrics getOverall() {
    return overall;
  }

  public DataMetrics getCurrent() {
    return current;
  }

  public Stopwatch getTimer() {
    return timer;
  }

  public Stopwatch getLast() {
    return last;
  }

  public Optional<String> getSchema() {
    return schema;
  }

  public String getTable() {
    return table;
  }

  public ImmutableList<String> getFields() {
    return fields;
  }

  public Function<ResultSet, T> getFunction() {
    return function;
  }

  public Function<T, Long> getWeigher() {
    return weigher;
  }

  public Optional<Integer> getShow() {
    return show;
  }

  public Optional<Integer> getMax() {
    return max;
  }

  public boolean isDiscard() {
    return discard;
  }

  public boolean isCloseConnection() {
    return closeConnection;
  }

}
