package ua.utility.kfsdbupgrade;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.lang.Integer.parseInt;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ua.utility.kfsdbupgrade.mdoc.BlockId;
import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.SingleStringFunction;
import ua.utility.kfsdbupgrade.mdoc.StringWeigher;

public class FirstTouchPenaltyTest {

  @Test
  public void test() {
    try {
      Properties props = new PropertiesProvider().get();
      List<RowId> rowIds = getRowIds(props, "KRNS_MAINT_DOC_T");
      Map<BlockId, RowId> blocks = getBlocks(rowIds);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private ImmutableMap<BlockId, RowId> getBlocks(Iterable<RowId> rowIds) {
    Map<BlockId, RowId> map = newLinkedHashMap();
    for (RowId rowId : rowIds) {
      map.put(rowId.getBlock(), rowId);
    }
    return copyOf(map);
  }

  private ImmutableList<RowId> getRowIds(Properties props, String table) {
    RowIdConverter converter = RowIdConverter.getInstance();
    ConnectionProvider provider = new ConnectionProvider(props, false);
    int max = parseInt(props.getProperty("rows.max"));
    int show = parseInt(props.getProperty("rows.show", max / 10 + ""));
    RowSelector.Builder<String> builder = RowSelector.builder();
    builder.withFunction(SingleStringFunction.INSTANCE);
    builder.withWeigher(StringWeigher.INSTANCE);
    builder.withMax(max);
    builder.withShow(show);
    builder.withTable(table);
    builder.withProvider(provider);
    RowSelector<String> selector = builder.build();
    List<String> strings = selector.get();
    return transform(strings, converter);
  }

}
