package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static ua.utility.kfsdbupgrade.md.base.Props.checkedValue;

import java.util.Properties;

import javax.inject.Provider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;

public final class DatabaseProvider implements Provider<String> {

  public DatabaseProvider(Properties props) {
    this.props = checkNotNull(props);
  }

  private final Properties props;

  @Override
  public String get() {
    String region = checkedValue(props, asList("aws.region", "AWS_DEFAULT_REGION"), "us-west-2");
    String snapshotDatabase = checkedValue(props, "db.snapshot.name");
    String instanceId = checkedValue(props, "db.name");
    AWSCredentials credentials = new CredentialsProvider(props).get();
    AmazonRDS rds = new AmazonRdsProvider(region, credentials).get();
    String snapshotId = new LatestSnapshotProvider(rds, snapshotDatabase, true).get();
    new DeleteDatabaseProvider(rds, instanceId, props).get();
    new CreateDatabaseProvider(rds, instanceId, snapshotId, props).get();
    new HardenDatabaseProvider(rds, instanceId, props).get();
    new RebootDatabaseProvider(rds, instanceId).get();
    return instanceId;
  }

}
