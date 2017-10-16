package ua.utility.kfsdbupgrade;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.md.base.Lists.transform;
import static ua.utility.kfsdbupgrade.md.base.Providers.of;
import static ua.utility.kfsdbupgrade.mdoc.Validation.checkNoBlanks;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.md.RowId;
import ua.utility.kfsdbupgrade.md.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.DatabaseMetrics;
import ua.utility.kfsdbupgrade.mdoc.MaintDoc;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.RowUpdateProvider;
import ua.utility.kfsdbupgrade.mdoc.RowUpdaterFunction;

public final class MaintDocCallable implements Callable<Long> {

  private final Provider<Connection> provider;
  private final ImmutableList<RowId> rowIds;
  private final int selectSize;
  private final RowUpdaterFunction function;
  private final Function<MaintDoc, MaintDoc> converter;
  private final DatabaseMetrics metrics;

  public Long call() {
    try {
      Stopwatch sw = createStarted();
      Connection conn = provider.get();
      for (List<RowId> partition : partition(rowIds, selectSize)) {
        RowSelector<MaintDoc> selector = getSelector(of(conn), partition, metrics);
        RowUpdateProvider<MaintDoc> updater = new RowUpdateProvider<>(selector, converter, function);
        updater.get();
      }
      conn.commit();
      return sw.elapsed(MILLISECONDS);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
  }

  private RowSelector<MaintDoc> getSelector(Provider<Connection> provider, List<RowId> ids, DatabaseMetrics metrics) {
    RowIdConverter converter = RowIdConverter.getInstance();
    List<String> rowIds = transform(ids, converter.reverse());
    RowSelector.Builder<MaintDoc> builder = RowSelector.builder();
    builder.withFunction(MaintDocFunction.INSTANCE);
    //builder.withWeigher(MaintDocWeigher.INSTANCE);
    builder.withRowIds(rowIds);
    // builder.withMetrics(metrics);
    builder.withTable("KRNS_MAINT_DOC_T");
    // builder.withProvider(provider);
    builder.withFields(asList("ROWID", "DOC_CNTNT"));
    // builder.withCloseConnection(false);
    // builder.withShowFinal(false);
    return builder.build();
  }

  private MaintDocCallable(Builder builder) {
    this.provider = builder.provider;
    this.rowIds = copyOf(builder.rowIds);
    this.selectSize = builder.selectSize;
    this.function = builder.function;
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
    private RowUpdaterFunction function;
    private Function<MaintDoc, MaintDoc> converter = identity();
    private DatabaseMetrics metrics;

    public Builder withMetrics(DatabaseMetrics metrics) {
      this.metrics = metrics;
      return this;
    }

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

    public Builder withFunction(RowUpdaterFunction function) {
      this.function = function;
      return this;
    }

    public Builder withConverter(Function<MaintDoc, MaintDoc> converter) {
      this.converter = converter;
      return this;
    }

    public MaintDocCallable build() {
      return checkNoBlanks(new MaintDocCallable(this));
    }

  }

}