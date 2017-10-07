package ua.utility.kfsdbupgrade;

import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;

import org.apache.log4j.Logger;
import org.junit.Test;

public class FirstTouchPenaltyTest {

  private static final Logger LOGGER = getLogger(FirstTouchPenaltyTest.class);

  @Test
  private void test() {
    try {
      info(LOGGER, "hello world");
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
