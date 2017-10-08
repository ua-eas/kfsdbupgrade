package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Stopwatch.createUnstarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Show.show;
import static ua.utility.kfsdbupgrade.mdoc.Stopwatches.synchronizedStart;
import static ua.utility.kfsdbupgrade.mdoc.Validation.checkNoBlanks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class RowUpdater<T> implements Provider<ImmutableList<T>> {

  private final Provider<Connection> provider;
  private final int batchSize;
  private final Function<BatchContext<T>, Long> batch;
  private final String where;
  private final ImmutableList<T> entities;
  private final DataMetrics overall;
  private final DataMetrics current;
  private final Stopwatch timer;
  private final Stopwatch last;
  private final Optional<String> schema;
  private final String table;
  private final ImmutableList<String> fields;
  private final Function<T, Long> weigher;
  private final Optional<Integer> show;

  @Override
  public ImmutableList<T> get() {
    synchronizedStart(timer, last);
    Connection conn = null;
    Statement stmt = null;
    PreparedStatement pstmt = null;
    try {
      conn = provider.get();
      stmt = conn.createStatement();
      String update = schema.isPresent() ? schema.get() + "." + table : table;
      String set = asSetClause(fields);
      pstmt = conn.prepareStatement(format("UPDATE %s SET %s WHERE %s = ?", update, set, where));
      for (List<T> partition : partition(entities, batchSize)) {
        for (T instance : partition) {
          Stopwatch sw = createStarted();
          batch.apply(new BatchContext<T>(instance, pstmt));
          long weight = weigher.apply(instance);
          sw = increment(1, weight, sw);
        }
        Stopwatch sw = createStarted();
        pstmt.executeBatch();
        increment(sw);
      }
      Stopwatch sw = createStarted();
      conn.commit();
      increment(sw);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      show(overall, current, timer, last, "done");
      closeQuietly(stmt);
      closeQuietly(pstmt);
      closeQuietly(conn);
    }
    return entities;
  }

  private String asSetClause(Iterable<String> fields) {
    Iterator<String> itr = fields.iterator();
    StringBuilder sb = new StringBuilder();
    while (itr.hasNext()) {
      sb.append(itr.next() + " = ?" + (itr.hasNext() ? "," : ""));
    }
    return sb.toString();
  }

  private Stopwatch increment(Stopwatch sw) {
    return increment(0, 0, sw);
  }

  private Stopwatch increment(long count, long weight, Stopwatch sw) {
    long micros = sw.elapsed(MICROSECONDS);
    synchronized (overall) {
      synchronized (current) {
        this.overall.increment(count, weight, micros);
        this.current.increment(count, weight, micros);
        if (show.isPresent() && overall.getCount() % show.get() == 0) {
          show(overall, current, timer, last);
          this.current.reset();
          this.last.reset().start();
        }
      }
    }
    return createStarted();
  }

  private RowUpdater(Builder<T> builder) {
    this.provider = builder.provider;
    this.batchSize = builder.batchSize;
    this.overall = builder.overall;
    this.timer = builder.timer;
    this.schema = builder.schema;
    this.table = builder.table;
    this.show = builder.show;
    this.fields = copyOf(builder.fields);
    this.weigher = builder.weigher;
    this.current = builder.current;
    this.batch = builder.batch;
    this.last = builder.last;
    this.where = builder.where;
    this.entities = copyOf(builder.entities);
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  public static class Builder<T> {

    private Provider<Connection> provider;
    private int batchSize = 75;
    private DataMetrics overall = new DataMetrics();
    private DataMetrics current = new DataMetrics();
    private Stopwatch timer = createUnstarted();
    private Optional<String> schema = absent();
    private String table;
    private List<String> fields = newArrayList("ROWID");
    private Optional<Integer> show = absent();
    private Function<T, Long> weigher;
    private Stopwatch last = createUnstarted();
    private Function<BatchContext<T>, Long> batch;
    private String where;
    private List<T> entities;

    public Builder<T> withWhere(String where) {
      this.where = where;
      return this;
    }

    public Builder<T> withBatch(Function<BatchContext<T>, Long> batch) {
      this.batch = batch;
      return this;
    }

    public Builder<T> withEntitities(List<T> entities) {
      this.entities = entities;
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

    public Builder<T> withWeigher(Function<T, Long> weigher) {
      this.weigher = weigher;
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

    public RowUpdater<T> build() {
      return validate(new RowUpdater<T>(this));
    }

    private static <T> RowUpdater<T> validate(RowUpdater<T> instance) {
      checkNoBlanks(instance);
      return instance;
    }

  }

}
