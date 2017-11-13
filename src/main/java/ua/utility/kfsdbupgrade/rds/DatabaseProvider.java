package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Provider;

import com.amazonaws.services.rds.AmazonRDS;

public final class DatabaseProvider implements Provider<String> {

  public DatabaseProvider(ProvideDatabaseRequest request) {
    this.request = checkNotNull(request);
  }

  private final ProvideDatabaseRequest request;

  @Override
  public String get() {
    AmazonRDS rds = new AmazonRdsProvider(request.getRegion()).get();
    // String snapshotId = new LatestSnapshotProvider(rds, request.getSnapshotDatabase(), true).get();
    // new DeleteDatabaseProvider(rds, request.getInstanceId()).get();
    // new CreateDatabaseProvider(rds, request.getInstanceId(), snapshotId).get();
    new HardenDatabaseProvider(rds, request.getInstanceId()).get();
    new RebootDatabaseProvider(rds, request.getInstanceId()).get();
    return request.getInstanceId();
  }

}
