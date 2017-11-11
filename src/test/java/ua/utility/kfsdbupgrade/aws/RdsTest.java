package ua.utility.kfsdbupgrade.aws;

import static com.google.common.base.Stopwatch.createStarted;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Strings.startsWith;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;

public final class RdsTest {

  private static final Logger LOGGER = getLogger(RdsTest.class);

  @Test
  public void test() {
    try {
      Stopwatch sw = createStarted();
      info(LOGGER, "connecting -> rds");
      AmazonRDS rds = AmazonRDSClient.builder().withRegion("us-west-2").build();
      info(LOGGER, "connected --> %s", getTime(sw));
      Predicate<String> filter = startsWith("rds:");
      String snapshotId = new LatestSnapshotProvider(rds, "kfs6upg", filter).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
