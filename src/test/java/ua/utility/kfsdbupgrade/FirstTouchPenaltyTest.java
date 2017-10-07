package ua.utility.kfsdbupgrade;

import static java.lang.Integer.parseInt;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

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
      System.setProperty("rows.max", "1000");
      Properties props = new PropertiesProvider().get();
      List<RowId> rowIds = getRowIds(props, "KRNS_MAINT_DOC_T");
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private ImmutableList<RowId> getRowIds(Properties props, String table) {
    RowIdConverter converter = RowIdConverter.getInstance();
    ConnectionProvider cp = new ConnectionProvider(props, false);
    int max = parseInt(props.getProperty("rows.max"));
    int show = parseInt(props.getProperty("rows.show", max / 10 + ""));
    RowSelector.Builder<String> builder = RowSelector.builder();
    builder.withFunction(SingleStringFunction.INSTANCE);
    builder.withWeigher(StringWeigher.INSTANCE);
    builder.withMax(max);
    builder.withShow(show);
    builder.withTable(table);
    builder.withProvider(cp);
    RowSelector<String> selector = builder.build();
    List<String> strings = selector.get();
    return transform(strings, converter);
  }

}
