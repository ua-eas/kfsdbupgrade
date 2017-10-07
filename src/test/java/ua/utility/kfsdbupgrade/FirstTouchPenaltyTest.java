package ua.utility.kfsdbupgrade;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ua.utility.kfsdbupgrade.mdoc.BlockId;
import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.IntegerWeigher;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.SingleIntegerFunction;
import ua.utility.kfsdbupgrade.mdoc.SingleStringFunction;
import ua.utility.kfsdbupgrade.mdoc.StringWeigher;

public class FirstTouchPenaltyTest {

  private static final Logger LOGGER = Logger.getLogger(FirstTouchPenaltyTest.class);

  @Test
  public void test() {
    try {
      String table = "KRNS_MAINT_DOC_T";
      Properties props = new PropertiesProvider().get();
      List<RowId> rowIds = getRowIds(props, table, 100000);
      Map<BlockId, RowId> blocks = getBlocks(rowIds);
      info(LOGGER, "rows ---> %s", getCount(rowIds.size()));
      info(LOGGER, "blocks -> %s", getCount(blocks.size()));
      touch(props, table, "VER_NBR", blocks.values());
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private ImmutableList<Integer> touch(Properties props, String table, String field, Iterable<RowId> rowIds) {
    ConnectionProvider provider = new ConnectionProvider(props, false);
    RowSelector.Builder<Integer> builder = RowSelector.builder();
    builder.withFunction(SingleIntegerFunction.INSTANCE);
    builder.withWeigher(IntegerWeigher.INSTANCE);
    builder.withShow(size(rowIds) / 100);
    builder.withTable(table);
    builder.withProvider(provider);
    RowSelector<Integer> selector = builder.build();
    return selector.get();
  }

  private ImmutableMap<BlockId, RowId> getBlocks(Iterable<RowId> rowIds) {
    Map<BlockId, RowId> map = newLinkedHashMap();
    for (RowId rowId : rowIds) {
      map.put(rowId.getBlock(), rowId);
    }
    return copyOf(map);
  }

  private ImmutableList<RowId> getRowIds(Properties props, String table, int show) {
    RowIdConverter converter = RowIdConverter.getInstance();
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
