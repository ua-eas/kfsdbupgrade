package ua.utility.kfsdbupgrade;

import static java.lang.Integer.parseInt;

import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import ua.utility.kfsdbupgrade.mdoc.BlockId;
import ua.utility.kfsdbupgrade.mdoc.BlockProvider;
import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.RowId;

public class FirstTouchPenaltyTest {

  @Test
  public void test() {
    try {
      System.setProperty("rows.max", "1000");
      Properties props = new PropertiesProvider().get();
      Map<BlockId, RowId> blocks = getBlocks(props, "KRNS_MAINT_DOC_T");
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private ImmutableMap<BlockId, RowId> getBlocks(Properties props, String table) {
    int max = parseInt(props.getProperty("rows.max"));
    int show = parseInt(props.getProperty("rows.show", max / 10 + ""));
    ConnectionProvider cp = new ConnectionProvider(props, false);
    BlockProvider.Builder builder = BlockProvider.builder();
    builder.withMax(max);
    builder.withShow(show);
    builder.withTable(table);
    builder.withProvider(cp);
    BlockProvider bp = builder.build();
    return bp.get();
  }

}
