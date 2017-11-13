package ua.utility.kfsdbupgrade.rds;

import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;

public final class ProvideDatabaseRequest {

  public ProvideDatabaseRequest(String region, String snapshotDatabase, String instanceId) {
    this.region = checkNotBlank(region, "region");
    this.snapshotDatabase = checkNotBlank(snapshotDatabase, "snapshotDatabase");
    this.instanceId = checkNotBlank(instanceId, "instanceId");
  }

  private final String region;
  private final String snapshotDatabase;
  private final String instanceId;

  public String getRegion() {
    return region;
  }

  public String getSnapshotDatabase() {
    return snapshotDatabase;
  }

  public String getInstanceId() {
    return instanceId;
  }

}
