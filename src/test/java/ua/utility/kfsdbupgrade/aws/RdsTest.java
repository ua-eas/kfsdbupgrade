package ua.utility.kfsdbupgrade.aws;

import org.junit.Test;

import com.amazonaws.services.rds.AmazonRDS;

public final class RdsTest {

  @Test
  public void test() {
    try {
      AmazonRDS rds = new AmazonRdsProvider("us-west-2").get();
      String snapshotId = new LatestSnapshotProvider(rds, "kfs3imp", true).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
