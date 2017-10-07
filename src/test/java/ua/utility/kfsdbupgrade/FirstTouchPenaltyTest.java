package ua.utility.kfsdbupgrade;

import static org.apache.log4j.Logger.getLogger;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Test;

import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;

public class FirstTouchPenaltyTest {

  private static final Logger LOGGER = getLogger(FirstTouchPenaltyTest.class);

  @Test
  public void test() {
    try {
      Properties props = new PropertiesProvider().get();
      ConnectionProvider cp = new ConnectionProvider(props, false);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
