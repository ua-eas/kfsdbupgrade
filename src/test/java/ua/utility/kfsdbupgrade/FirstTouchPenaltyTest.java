package ua.utility.kfsdbupgrade;

import static com.google.common.base.Stopwatch.createUnstarted;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.mdoc.Callables.getFutures;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocField.DOC_CNTNT;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocField.VER_NBR;
import static ua.utility.kfsdbupgrade.mdoc.Providers.of;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ua.utility.kfsdbupgrade.mdoc.BlockId;
import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.DataMetrics;
import ua.utility.kfsdbupgrade.mdoc.ExecutorProvider;
import ua.utility.kfsdbupgrade.mdoc.IntegerWeigher;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.SingleIntegerFunction;
import ua.utility.kfsdbupgrade.mdoc.SingleStringFunction;
import ua.utility.kfsdbupgrade.mdoc.StringWeigher;
import ua.utility.kfsdbupgrade.mdoc.ThreadsProvider;

public class FirstTouchPenaltyTest {

  private static final Logger LOGGER = Logger.getLogger(FirstTouchPenaltyTest.class);

  private final RowIdConverter converter = RowIdConverter.getInstance();

  @Test
  public void test() {
    try {
      String table = "KRNS_MAINT_DOC_T";
      Properties props = new PropertiesProvider().get();
      List<RowId> rowIds = getRowIds(props, table, 100000);
      Map<BlockId, RowId> blocks = getBlocks(rowIds);
      info(LOGGER, "rows ---> %s", getCount(rowIds.size()));
      info(LOGGER, "blocks -> %s", getCount(blocks.size()));
      touch(props, table, VER_NBR.name(), blocks.values(), SingleIntegerFunction.INSTANCE, IntegerWeigher.INSTANCE, 10);
      touch(props, table, DOC_CNTNT.name(), rowIds, SingleStringFunction.INSTANCE, StringWeigher.INSTANCE, 1000);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private <T> void touch(Properties props, String table, String field, Iterable<RowId> iterable, Function<ResultSet, T> function, Function<T, Long> weigher, int divisor) {
    List<String> rowIds = transform(iterable, converter.reverse());
    int threads = new ThreadsProvider(props).get();
    ExecutorService executor = new ExecutorProvider("touch", threads).get();
    List<Callable<ImmutableList<T>>> callables = newArrayList();
    DataMetrics metrics = new DataMetrics();
    Stopwatch timer = createUnstarted();
    for (List<String> distribution : distribute(rowIds, threads)) {
      Provider<Connection> provider = of(new ConnectionProvider(props, false).get());
      RowSelector.Builder<T> builder = RowSelector.builder();
      builder.withFunction(function);
      builder.withWeigher(weigher);
      builder.withRowIds(distribution);
      builder.withShow(rowIds.size() / divisor);
      builder.withTable(table);
      builder.withProvider(provider);
      builder.withField(field);
      builder.withDiscard(true);
      builder.withMetrics(metrics);
      builder.withTimer(timer);
      RowSelector<T> selector = builder.build();
      callables.add(fromProvider(selector));
    }
    getFutures(executor, callables);
  }

  private ImmutableMap<BlockId, RowId> getBlocks(Iterable<RowId> rowIds) {
    Map<BlockId, RowId> map = newLinkedHashMap();
    for (RowId rowId : rowIds) {
      map.put(rowId.getBlock(), rowId);
    }
    return copyOf(map);
  }

  private ImmutableList<RowId> getRowIds(Properties props, String table, int show) {
    ConnectionProvider provider = new ConnectionProvider(props, false);
    RowSelector.Builder<String> builder = RowSelector.builder();
    builder.withFunction(SingleStringFunction.INSTANCE);
    builder.withWeigher(StringWeigher.INSTANCE);
    builder.withShow(show);
    builder.withTable(table);
    builder.withProvider(provider);
    RowSelector<String> selector = builder.build();
    List<String> strings = selector.get();
    return transform(strings, converter);
  }

}
