package ua.utility.kfsdbupgrade;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Lists.shuffle;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;
import static ua.utility.kfsdbupgrade.mdoc.Providers.of;
import static ua.utility.kfsdbupgrade.mdoc.Validation.checkNoBlanks;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.DataMetrics;
import ua.utility.kfsdbupgrade.mdoc.MaintDoc;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.RowUpdateProvider;

public final class MaintDocCallable implements Callable<Long> {

  private final Provider<Connection> provider;
  private final ImmutableList<RowId> rowIds;
  private final int batchSize;
  private final int show;
  private final RowUpdaterFunction function;
  private final Function<MaintDoc, MaintDoc> converter;
  private final DataMetrics overall;
  private final DataMetrics current;
  private final Stopwatch timer;
  private final Stopwatch last;

  public Long call() {
    try {
      Stopwatch sw = createStarted();
      Connection conn = provider.get();
      for (List<RowId> partition : partition(rowIds, batchSize)) {
        RowSelector<MaintDoc> selector = getSelector(of(conn), partition);
        RowUpdateProvider<MaintDoc> updater = new RowUpdateProvider<>(selector, converter, function);
        updater.get();
      }
      conn.commit();
      return sw.elapsed(MILLISECONDS);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
  }

  private RowSelector<MaintDoc> getSelector(Provider<Connection> provider, List<RowId> ids) {
    RowIdConverter converter = RowIdConverter.getInstance();
    List<String> rowIds = shuffle(transform(ids, converter.reverse()));
    RowSelector.Builder<MaintDoc> builder = RowSelector.builder();
    builder.withFunction(MaintDocFunction.INSTANCE);
    builder.withWeigher(MaintDocWeigher.INSTANCE);
    builder.withOverall(overall);
    builder.withCurrent(current);
    builder.withTimer(timer);
    builder.withLast(last);
    builder.withRowIds(rowIds);
    builder.withShow(show);
    builder.withTable("KRNS_MAINT_DOC_T");
    builder.withProvider(provider);
    builder.withFields(asList("ROWID", "DOC_CNTNT"));
    builder.withCloseConnection(false);
    return builder.build();
  }

  private MaintDocCallable(Builder builder) {
    this.provider = builder.provider;
    this.rowIds = copyOf(builder.rowIds);
    this.batchSize = builder.batchSize;
    this.show = builder.show;
    this.function = builder.function;
    this.converter = builder.converter;
    this.overall = builder.overall;
    this.current = builder.current;
    this.timer = builder.timer;
    this.last = builder.last;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Provider<Connection> provider;
    private List<RowId> rowIds;
    private int batchSize;
    private int show;
    private RowUpdaterFunction function;
    private Function<MaintDoc, MaintDoc> converter = identity();
    private DataMetrics overall;
    private DataMetrics current;
    private Stopwatch timer;
    private Stopwatch last;

    public Builder withProvider(Provider<Connection> provider) {
      this.provider = provider;
      return this;
    }

    public Builder withRowIds(List<RowId> rowIds) {
      this.rowIds = rowIds;
      return this;
    }

    public Builder withBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder withShow(int show) {
      this.show = show;
      return this;
    }

    public Builder withFunction(RowUpdaterFunction function) {
      this.function = function;
      return this;
    }

    public Builder withConverter(Function<MaintDoc, MaintDoc> converter) {
      this.converter = converter;
      return this;
    }

    public Builder withOverall(DataMetrics overall) {
      this.overall = overall;
      return this;
    }

    public Builder withCurrent(DataMetrics current) {
      this.current = current;
      return this;
    }

    public Builder withTimer(Stopwatch timer) {
      this.timer = timer;
      return this;
    }

    public Builder withLast(Stopwatch last) {
      this.last = last;
      return this;
    }

    public MaintDocCallable build() {
      return checkNoBlanks(new MaintDocCallable(this));
    }

  }

}