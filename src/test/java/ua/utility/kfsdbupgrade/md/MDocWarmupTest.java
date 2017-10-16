package ua.utility.kfsdbupgrade.md;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static ua.utility.kfsdbupgrade.md.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.md.base.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.md.base.Callables.getFutures;
import static ua.utility.kfsdbupgrade.md.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.md.base.Lists.distribute;
import static ua.utility.kfsdbupgrade.md.base.Lists.shuffle;
import static ua.utility.kfsdbupgrade.md.base.Lists.transform;
import static ua.utility.kfsdbupgrade.md.base.Logging.info;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ua.utility.kfsdbupgrade.mdoc.DatabaseMetrics;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.SingleStringFunction;

public class MDocWarmupTest {

  private static final Logger LOGGER = Logger.getLogger(MDocWarmupTest.class);

  private final RowIdConverter converter = RowIdConverter.getInstance();

  @Test
  public void test() {
    Connection conn = null;
    try {
      Properties props = new PropertiesProvider().get();
      conn = new ConnectionProvider(props, false).get();
      String table = "KRNS_MAINT_DOC_T";
      List<String> strings = RowIdProvider.build(conn, table, 1000000, 100000).get();
      List<RowId> rowIds = shuffle(transform(strings, RowIdConverter.getInstance()));
      Map<BlockId, RowId> blocks = getBlocks(rowIds);
      double select = ((blocks.size() * 1d) / rowIds.size() * 100);
      info(LOGGER, "rows ------> %s", getCount(rowIds.size()));
      info(LOGGER, "blocks ----> %s", getCount(blocks.size()));
      info(LOGGER, "selecting -> %s%% of the total number of rows", getCount(checkedCast(round(select))));
      // touch(props, table, VER_NBR.name(), blocks.values(), SingleIntegerFunction.INSTANCE, IntegerWeigher.INSTANCE, 10000);
      int maximum = min(rowIds.size(), parseInt(props.getProperty("mdoc.clobs", "30000")));
      // touch(props, table, DOC_CNTNT.name(), rowIds.subList(0, maximum), SingleStringFunction.INSTANCE, StringWeigher.INSTANCE, 1000);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(conn);
    }
  }

  private static <T> void touch(List<Connection> conns, ExecutorService executor, String table, String field, Iterable<RowId> iterable, Function<ResultSet, T> function) {
    RowIdConverter converter = RowIdConverter.getInstance();
    List<String> rowIds = shuffle(transform(iterable, converter.reverse()));
    List<Callable<ImmutableList<T>>> callables = newArrayList();
    int index = 0;
    for (List<String> distribution : distribute(rowIds, conns.size())) {
      RowSelector.Builder<T> builder = RowSelector.builder();
      builder.withFunction(function);
      builder.withRowIds(distribution);
      builder.withTable(table);
      builder.withConn(conns.get(index++));
      builder.withField(field);
      builder.withDiscard(true);
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
    DatabaseMetrics metrics = new DatabaseMetrics(show, false);
    RowSelector.Builder<String> builder = RowSelector.builder();
    builder.withFunction(SingleStringFunction.INSTANCE);
    // builder.withWeigher(StringWeigher.INSTANCE);
    // builder.withMetrics(metrics);
    builder.withTable(table);
    // builder.withProvider(provider);
    RowSelector<String> selector = builder.build();
    metrics.start();
    return transform(selector.get(), converter);
  }

  protected void addDocumentContentIndex(Properties props) throws IOException {
    Connection conn = null;
    Statement stmt = null;
    try {
      info(LOGGER, "adding clob index for krns_maint_doc_t.doc_cntnt");
      conn = new ConnectionProvider(props, true).get();
      stmt = conn.createStatement();
      stmt.execute("ALTER TABLE KRNS_MAINT_DOC_T MODIFY LOB (DOC_CNTNT) (CACHE)");
    } catch (Throwable e) {
      throw new IOException(e);
    } finally {
      closeQuietly(stmt);
      closeQuietly(conn);
    }

  }

}
