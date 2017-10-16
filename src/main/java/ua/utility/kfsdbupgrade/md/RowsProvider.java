package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.md.base.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.md.base.Callables.getFutures;
import static ua.utility.kfsdbupgrade.md.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.md.base.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.md.base.Lists.concat;
import static ua.utility.kfsdbupgrade.md.base.Lists.distribute;
import static ua.utility.kfsdbupgrade.md.base.Logging.info;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class RowsProvider<T> implements Provider<ImmutableList<T>> {

  private static final Logger LOGGER = getLogger(RowsProvider.class);

  private final ImmutableList<Connection> conns;
  private final int chunkSize;
  private final int selectSize;
  private final boolean discard;
  private final Optional<String> schema;
  private final String table;
  private final ImmutableList<String> fields;
  private final ImmutableList<String> rowIds;
  private final Function<ResultSet, T> function;
  private final ExecutorService executor;

  @Override
  public ImmutableList<T> get() {
    try {
      Stopwatch overall = createStarted();
      String from = schema.isPresent() ? schema.get() + "." + table : table;
      info(LOGGER, "selecting [%s] from %s, %s rows total, discard=%s", Joiner.on(',').join(fields), from, getCount(rowIds.size()), discard);
      List<T> list = newArrayList();
      int chunkId = 0;
      for (List<String> chunk : distribute(rowIds, chunkSize)) {
        Stopwatch current = createStarted();
        int index = 0;
        List<Callable<ImmutableList<T>>> callables = newArrayList();
        for (List<String> distribution : distribute(chunk, conns.size())) {
          RowProvider<T> provider = RowProvider.build(conns.get(index++), table, fields, distribution, selectSize, discard);
          callables.add(fromProvider(provider));
        }
        list.addAll(concat(getFutures(executor, callables)));
        chunkId++;
        String throughput = getThroughputInSeconds(current.elapsed(MILLISECONDS), chunk.size(), "rows/sec");
        info(LOGGER, "processed %s chunks of %s total, %s", getCount(chunkId), getCount(rowIds.size() / chunkSize), throughput);
      }
      String throughput = getThroughputInSeconds(overall.elapsed(MILLISECONDS), rowIds.size(), "rows/sec");
      info(LOGGER, "processed %s chunks of %s total, %s", getCount(chunkId), getCount(rowIds.size() / chunkSize), throughput);
      return copyOf(list);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
  }

  private RowsProvider(Builder<T> builder) {
    this.conns = copyOf(builder.conns);
    this.selectSize = builder.selectSize;
    this.schema = builder.schema;
    this.table = builder.table;
    this.rowIds = copyOf(builder.rowIds);
    this.fields = copyOf(builder.fields);
    this.function = builder.function;
    this.discard = builder.discard;
    this.executor = builder.executor;
    this.chunkSize = builder.chunkSize;
  }

  public static <T> RowsProvider<T> build(Iterable<Connection> conns, String table, String field, Iterable<String> rowIds, int chunkSize, int selectSize, boolean discard) {
    Builder<T> builder = builder();
    builder.withConns(copyOf(conns));
    builder.withTable(table);
    builder.withField(field);
    builder.withRowIds(copyOf(rowIds));
    builder.withChunkSize(chunkSize);
    builder.withSelectSize(selectSize);
    builder.withDiscard(discard);
    return builder.build();
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  public static class Builder<T> {

    private List<Connection> conns;
    private int chunkSize = -1;
    private int selectSize = -1;
    private boolean discard;
    private Optional<String> schema = absent();
    private String table;
    private List<String> rowIds;
    private Function<ResultSet, T> function;
    private List<String> fields;
    private ExecutorService executor;

    public Builder<T> withChunkSize(int chunkSize) {
      this.chunkSize = chunkSize;
      return this;
    }

    public Builder<T> withExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    public Builder<T> withField(String field) {
      return withFields(asList(field));
    }

    public Builder<T> withFields(List<String> fields) {
      this.fields = fields;
      return this;
    }

    public Builder<T> withRowIds(List<String> rowIds) {
      this.rowIds = rowIds;
      return this;
    }

    public Builder<T> withConns(List<Connection> conns) {
      this.conns = conns;
      return this;
    }

    public Builder<T> withDiscard(boolean discard) {
      this.discard = discard;
      return this;
    }

    public Builder<T> withSelectSize(int selectSize) {
      this.selectSize = selectSize;
      return this;
    }

    public Builder<T> withTable(String table) {
      this.table = table;
      return this;
    }

    public Builder<T> withSchema(String schema) {
      return withSchema(of(schema));
    }

    public Builder<T> withSchema(Optional<String> schema) {
      this.schema = schema;
      return this;
    }

    public RowsProvider<T> build() {
      return validate(new RowsProvider<T>(this));
    }

    private static <T> RowsProvider<T> validate(RowsProvider<T> instance) {
      checkNotNull(instance.schema, "schema may not be null");
      checkNotNull(instance.table, "table may not be null");
      checkNotNull(instance.function, "function may not be null");
      checkNotNull(instance.executor, "executor may not be null");
      checkArgument(instance.conns.size() > 0, "conns can't be empty");
      checkArgument(instance.selectSize > 0, "select size must be greater than zero");
      checkArgument(instance.chunkSize > 0, "chunk size must be greater than zero");
      checkArgument(instance.rowIds.size() > 0, "rowIds can't be empty");
      checkArgument(instance.fields.size() > 0, "fields can't be empty");
      return instance;
    }
  }

}
