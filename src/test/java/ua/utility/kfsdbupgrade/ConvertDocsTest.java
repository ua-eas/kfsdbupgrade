package ua.utility.kfsdbupgrade;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createUnstarted;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static ua.utility.kfsdbupgrade.mdoc.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.mdoc.Callables.getFutures;
import static ua.utility.kfsdbupgrade.mdoc.Exceptions.illegalState;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;
import static ua.utility.kfsdbupgrade.mdoc.Lists.shuffle;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;
import static ua.utility.kfsdbupgrade.mdoc.Providers.of;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.DataMetrics;
import ua.utility.kfsdbupgrade.mdoc.DocBatchFunction;
import ua.utility.kfsdbupgrade.mdoc.ExecutorProvider;
import ua.utility.kfsdbupgrade.mdoc.MaintDoc;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.RowUpdateProvider;
import ua.utility.kfsdbupgrade.mdoc.RowUpdater;
import ua.utility.kfsdbupgrade.mdoc.SelectContext;
import ua.utility.kfsdbupgrade.mdoc.SingleStringFunction;
import ua.utility.kfsdbupgrade.mdoc.StringWeigher;
import ua.utility.kfsdbupgrade.mdoc.ThreadsProvider;

public class ConvertDocsTest {

  @Test
  public void test() {
    try {
      Properties props = new PropertiesProvider().get();
      int threads = new ThreadsProvider(props).get();
      ExecutorService executor = new ExecutorProvider("mdoc", threads).get();
      String table = "KRNS_MAINT_DOC_T";
      int max = 10000;
      int show = max / 10;
      List<RowId> ids = getRowIds(props, table, max, show);
      DataMetrics overall = new DataMetrics();
      DataMetrics current = new DataMetrics();
      List<RowSelector<MaintDoc>> selectors = getSelectors(props, ids, threads, show);
      List<Callable<ImmutableList<MaintDoc>>> callables = newArrayList();
      for (RowSelector<MaintDoc> selector : selectors) {
        Callable<ImmutableList<MaintDoc>> callable = fromProvider(selector);
        callables.add(callable);
      }
      getFutures(executor, callables);
      if (true) {
        return;
      }

      List<RowUpdateProvider<MaintDoc>> updaters = newArrayList();
      Function<MaintDoc, MaintDoc> converter = identity();
      RowUpdaterFunction function = new RowUpdaterFunction(show, overall, current);
      for (RowSelector<MaintDoc> selector : selectors) {
        RowUpdateProvider<MaintDoc> updater = new RowUpdateProvider<>(selector, converter, function);
        updaters.add(updater);
      }
      // List<Callable<ImmutableList<MaintDoc>>> callables = newArrayList();
      // for (RowUpdateProvider<MaintDoc> updater : updaters) {
      // Callable<ImmutableList<MaintDoc>> callable = fromProvider(updater);
      // callables.add(callable);
      // }
      // getFutures(executor, callables);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private static class RowUpdaterFunction implements Function<SelectContext<MaintDoc>, RowUpdater<MaintDoc>> {

    public RowUpdaterFunction(int show, DataMetrics overall, DataMetrics current) {
      this.show = show;
      this.overall = checkNotNull(overall);
      this.current = checkNotNull(current);
    }

    private final int show;
    private final DataMetrics overall;
    private final DataMetrics current;

    public RowUpdater<MaintDoc> apply(SelectContext<MaintDoc> input) {
      RowSelector<MaintDoc> selector = input.getSelector();
      RowUpdater.Builder<MaintDoc> builder = RowUpdater.builder();
      builder.withProvider(selector.getProvider());
      builder.withTable(selector.getTable());
      builder.withField("DOC_CNTNT");
      builder.withWeigher(selector.getWeigher());
      builder.withBatch(DocBatchFunction.INSTANCE);
      builder.withWhere("ROWID");
      builder.withEntitities(input.getSelected());
      builder.withShow(show);
      builder.withBatchSize(3);
      builder.withOverall(overall);
      builder.withCurrent(current);
      return builder.build();
    }
  }

  // setup selectors that extract maintenance documents
  private static ImmutableList<RowSelector<MaintDoc>> getSelectors(Properties props, List<RowId> ids, int threads, int show) {
    RowIdConverter converter = RowIdConverter.getInstance();
    List<String> rowIds = shuffle(transform(ids, converter.reverse()));
    DataMetrics overallSelect = new DataMetrics();
    DataMetrics currentSelect = new DataMetrics();
    DataMetrics overallUpdate = new DataMetrics();
    DataMetrics currentUpdate = new DataMetrics();
    Stopwatch timer = createUnstarted();
    Stopwatch last = createUnstarted();
    RowUpdaterFunction updater = new RowUpdaterFunction(show, overallUpdate, currentUpdate);
    List<RowSelector<MaintDoc>> list = newArrayList();
    for (List<String> distribution : distribute(rowIds, threads)) {
      Provider<Connection> provider = of(new ConnectionProvider(props, false).get());
      RowSelector.Builder<MaintDoc> builder = RowSelector.builder();
      builder.withFunction(MaintDocFunction.INSTANCE);
      builder.withWeigher(MaintDocWeigher.INSTANCE);
      builder.withRowIds(distribution);
      builder.withShow(show);
      builder.withTable("KRNS_MAINT_DOC_T");
      builder.withProvider(provider);
      builder.withFields(asList("ROWID", "DOC_CNTNT"));
      builder.withOverall(overallSelect);
      builder.withCurrent(currentSelect);
      builder.withTimer(timer);
      builder.withLast(last);
      builder.withCloseConnection(true);
      builder.withUpdater(updater);
      RowSelector<MaintDoc> selector = builder.build();
      list.add(selector);
    }
    return newList(list);
  }

  private enum MaintDocFunction implements Function<ResultSet, MaintDoc> {
    INSTANCE;

    public MaintDoc apply(ResultSet rs) {
      try {
        String id = rs.getString(1);
        String content = rs.getString(2);
        return MaintDoc.build(id, content);
      } catch (SQLException e) {
        throw illegalState(e);
      }
    }
  }

  private enum MaintDocWeigher implements Function<MaintDoc, Long> {
    INSTANCE;

    public Long apply(MaintDoc input) {
      return 0L + input.getId().length() + input.getContent().length();
    }
  }

  private ImmutableList<RowId> getRowIds(Properties props, String table, int max, int show) {
    ConnectionProvider provider = new ConnectionProvider(props, false);
    RowSelector.Builder<String> builder = RowSelector.builder();
    builder.withFunction(SingleStringFunction.INSTANCE);
    builder.withWeigher(StringWeigher.INSTANCE);
    builder.withMax(max);
    builder.withShow(show);
    builder.withTable(table);
    builder.withProvider(provider);
    RowSelector<String> selector = builder.build();
    List<String> strings = selector.get();
    return transform(strings, RowIdConverter.getInstance());
  }

}
