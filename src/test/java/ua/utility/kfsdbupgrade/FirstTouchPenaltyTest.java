package ua.utility.kfsdbupgrade;

import static org.apache.log4j.Logger.getLogger;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Optional;

import ua.utility.kfsdbupgrade.mdoc.BlockProvider;
import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;

public class FirstTouchPenaltyTest {

  private static final Logger LOGGER = getLogger(FirstTouchPenaltyTest.class);

  @Test
  public void test() {
    try {
      Properties props = new PropertiesProvider().get();
      ConnectionProvider cp = new ConnectionProvider(props, false);
      BlockProvider.Builder builder = BlockProvider.builder();
      builder.withMax(Optional.<Integer>of(100));
      builder.withTable("KRNS_MAINT_DOC_T");
      builder.withProvider(cp);

    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
