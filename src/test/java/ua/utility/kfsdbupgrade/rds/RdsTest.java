package ua.utility.kfsdbupgrade.rds;

import org.junit.Test;

import com.amazonaws.services.rds.AmazonRDS;

public final class RdsTest {

  @Test
  public void test() {
    try {
      String region = "us-west-2";
      String snapshotDatabase = "kfs3imp";
      String instanceId = "kfs36014";
      AmazonRDS rds = new AmazonRdsProvider(region).get();
      String snapshotId = new LatestSnapshotProvider(rds, snapshotDatabase, true).get();
      new DeleteDatabaseProvider(rds, instanceId).get();
      new RestoreDatabaseProvider(rds, instanceId, snapshotId).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
