package ua.utility.kfsdbupgrade;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.partition;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Lists.shuffle;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;
import static ua.utility.kfsdbupgrade.mdoc.Providers.of;

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

  private Provider<Connection> provider;
  private ImmutableList<RowId> rowIds;
  private int batchSize;
  private int show;
  private RowUpdaterFunction function;
  private Function<MaintDoc, MaintDoc> converter = identity();
  private DataMetrics overall;
  private DataMetrics current;
  private Stopwatch timer;
  private Stopwatch last;

  public Long call() {
    Stopwatch sw = createStarted();
    Connection conn = provider.get();
    for (List<RowId> partition : partition(rowIds, batchSize)) {
      RowSelector<MaintDoc> selector = getSelector(of(conn), partition, overall, current, timer, last, show);
      RowUpdateProvider<MaintDoc> updater = new RowUpdateProvider<>(selector, converter, function);
      updater.get();
    }
    return sw.elapsed(MILLISECONDS);
  }

  private static RowSelector<MaintDoc> getSelector(Provider<Connection> provider, List<RowId> ids, DataMetrics overall, DataMetrics current, Stopwatch timer, Stopwatch last,
      int show) {
    RowIdConverter converter = RowIdConverter.getInstance();
    List<String> rowIds = shuffle(transform(ids, converter.reverse()));
    RowSelector.Builder<MaintDoc> builder = RowSelector.builder();
    builder.withFunction(MaintDocFunction.INSTANCE);
    builder.withWeigher(MaintDocWeigher.INSTANCE);
    builder.withRowIds(rowIds);
    builder.withShow(show);
    builder.withTable("KRNS_MAINT_DOC_T");
    builder.withProvider(provider);
    builder.withFields(asList("ROWID", "DOC_CNTNT"));
    builder.withOverall(overall);
    builder.withCurrent(current);
    builder.withTimer(timer);
    builder.withLast(last);
    builder.withCloseConnection(false);
    return builder.build();
  }

}