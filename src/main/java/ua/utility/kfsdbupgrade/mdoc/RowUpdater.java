package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static ua.utility.kfsdbupgrade.md.Closeables.closeQuietly;
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
  private final Optional<String> schema;
  private final String table;
  private final ImmutableList<String> fields;
  private final Function<T, Long> weigher;
  private final boolean closeConnection;
  private final DatabaseMetrics metrics;

  @Override
  public ImmutableList<T> get() {
    Connection conn = null;
    Statement stmt = null;
    PreparedStatement pstmt = null;
    try {
      conn = provider.get();
      stmt = conn.createStatement();
      String update = schema.isPresent() ? schema.get() + "." + table : table;
      String set = asSetClause(fields);
      String sql = format("UPDATE %s SET %s WHERE %s = ?", update, set, where);
      pstmt = conn.prepareStatement(sql);
      for (List<T> partition : partition(entities, batchSize)) {
        for (T instance : partition) {
          Stopwatch sw = createStarted();
          batch.apply(new BatchContext<T>(instance, pstmt));
          long weight = weigher.apply(instance);
          sw = increment(weight, sw);
        }
        Stopwatch sw = createStarted();
        pstmt.executeBatch();
        increment(sw);
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(stmt);
      closeQuietly(pstmt);
      if (closeConnection) {
        closeQuietly(conn);
      }
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
    metrics.update(sw.elapsed(MICROSECONDS));
    return createStarted();
  }

  private Stopwatch increment(long weight, Stopwatch sw) {
    long micros = sw.elapsed(MICROSECONDS);
    metrics.update(weight, micros);
    return createStarted();
  }

  private RowUpdater(Builder<T> builder) {
    this.provider = builder.provider;
    this.batchSize = builder.batchSize;
    this.schema = builder.schema;
    this.table = builder.table;
    this.fields = copyOf(builder.fields);
    this.weigher = builder.weigher;
    this.batch = builder.batch;
    this.where = builder.where;
    this.closeConnection = builder.closeConnection;
    this.metrics = builder.metrics;
    this.entities = copyOf(builder.entities);
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  public static class Builder<T> {

    private int batchSize = 75;
    private Optional<String> schema = absent();
    private Provider<Connection> provider;
    private String table;
    private List<String> fields;
    private Function<T, Long> weigher;
    private Function<BatchContext<T>, Long> batch;
    private String where;
    private List<T> entities;
    private boolean closeConnection;
    private DatabaseMetrics metrics;

    public Builder<T> withMetrics(DatabaseMetrics metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder<T> withCloseConnection(boolean closeConnection) {
      this.closeConnection = closeConnection;
      return this;
    }

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

    public RowUpdater<T> build() {
      return validate(new RowUpdater<T>(this));
    }

    private static <T> RowUpdater<T> validate(RowUpdater<T> instance) {
      checkNoBlanks(instance);
      return instance;
    }

  }

}
