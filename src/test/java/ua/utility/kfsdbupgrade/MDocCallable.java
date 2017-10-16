package ua.utility.kfsdbupgrade;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.md.base.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.md.base.Callables.getFutures;
import static ua.utility.kfsdbupgrade.md.base.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;
import static ua.utility.kfsdbupgrade.md.base.Lists.transform;
import static ua.utility.kfsdbupgrade.md.base.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Providers.fromFunction;
import static ua.utility.kfsdbupgrade.md.base.Providers.of;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.md.ExecutorProvider;
import ua.utility.kfsdbupgrade.md.RowId;
import ua.utility.kfsdbupgrade.md.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.DatabaseMetrics;
import ua.utility.kfsdbupgrade.mdoc.MaintDoc;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;

public final class MDocCallable implements Callable<Long> {

  private static final Logger LOGGER = Logger.getLogger(MDocCallable.class);

  private final Provider<Connection> provider;
  private final ImmutableList<RowId> rowIds;
  private final int selectSize;
  private final Function<MaintDoc, MaintDoc> converter;
  private final DatabaseMetrics metrics;
  private final int threads = getRuntime().availableProcessors();

  public Long call() {
    try {
      Stopwatch sw = createStarted();
      Connection conn = provider.get();
      ExecutorService executor = new ExecutorProvider("c", threads).get();
      for (List<RowId> partition : partition(rowIds, selectSize)) {
        RowSelector<MaintDoc> selector = getSelector(of(conn), partition);
        List<MaintDoc> originals = selector.get();
        Stopwatch conversion = createStarted();
        List<MaintDoc> converted = doConversion(executor, originals);
        String tp = getThroughputInSeconds(conversion, converted.size(), "docs/sec");
        info(LOGGER, "converted -> %s", tp);

      }
      conn.commit();
      return sw.elapsed(MILLISECONDS);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
  }

  private ImmutableList<MaintDoc> doConversion(ExecutorService executor, List<MaintDoc> docs) {
    List<Callable<MaintDoc>> callables = newArrayList();
    for (MaintDoc doc : docs) {
      Provider<MaintDoc> provider = fromFunction(doc, converter);
      Callable<MaintDoc> callable = fromProvider(provider);
      callables.add(callable);
    }
    return getFutures(executor, callables);
  }

  private RowSelector<MaintDoc> getSelector(Provider<Connection> provider, List<RowId> ids) {
    RowIdConverter converter = RowIdConverter.getInstance();
    List<String> rowIds = transform(ids, converter.reverse());
    RowSelector.Builder<MaintDoc> builder = RowSelector.builder();
    builder.withFunction(MaintDocFunction.INSTANCE);
    builder.withWeigher(MaintDocWeigher.INSTANCE);
    builder.withRowIds(rowIds);
    builder.withTable("KRNS_MAINT_DOC_T");
    builder.withProvider(provider);
    builder.withFields(asList("ROWID", "DOC_CNTNT"));
    builder.withCloseConnection(false);
    builder.withShowFinal(false);
    builder.withMetrics(metrics);
    return builder.build();
  }

  private MDocCallable(Builder builder) {
    this.provider = builder.provider;
    this.rowIds = newList(builder.rowIds);
    this.selectSize = builder.selectSize;
    this.converter = builder.converter;
    this.metrics = builder.metrics;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Provider<Connection> provider;
    private List<RowId> rowIds;
    private int selectSize;
    private Function<MaintDoc, MaintDoc> converter;
    private DatabaseMetrics metrics;

    public Builder withProvider(Provider<Connection> provider) {
      this.provider = provider;
      return this;
    }

    public Builder withRowIds(List<RowId> rowIds) {
      this.rowIds = rowIds;
      return this;
    }

    public Builder withSelectSize(int selectSize) {
      this.selectSize = selectSize;
      return this;
    }

    public Builder withConverter(Function<MaintDoc, MaintDoc> converter) {
      this.converter = converter;
      return this;
    }

    public Builder withMetrics(DatabaseMetrics metrics) {
      this.metrics = metrics;
      return this;
    }

    public MDocCallable build() {
      return validate(new MDocCallable(this));
    }

    private static MDocCallable validate(MDocCallable instance) {
      checkNotNull(instance.provider, "provider may not be null");
      checkNotNull(instance.rowIds, "rowIds may not be null");
      checkArgument(instance.selectSize > 0, "selectSize should be set");
      checkNotNull(instance.converter, "converter may not be null");
      checkNotNull(instance.metrics, "metrics may not be null");
      return instance;
    }
  }

}