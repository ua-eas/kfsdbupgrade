package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.util.Arrays.asList;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Props.checkedValue;

import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;
import com.google.common.base.Stopwatch;

public final class DatabaseProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(DatabaseProvider.class);

  public DatabaseProvider(Properties props) {
    this.props = checkNotNull(props);
  }

  private final Properties props;

  @Override
  public String get() {
    Stopwatch sw = createStarted();
    info(LOGGER, "provisioning new database");
    String region = checkedValue(props, asList("aws.region", "AWS_DEFAULT_REGION"), "us-west-2");
    String snapshotDatabase = checkedValue(props, "db.snapshot.name");
    String instanceId = checkedValue(props, "db.name");
    AWSCredentials credentials = new CredentialsProvider(props).get();
    AmazonRDS rds = new AmazonRdsProvider(region, credentials).get();
    String snapshotId = new LatestSnapshotProvider(rds, snapshotDatabase, true).get();
    new DeleteDatabaseProvider(rds, instanceId, props).get();
    new CreateDatabaseProvider(rds, instanceId, snapshotId, props).get();
    new FinalizeDatabaseProvider(rds, instanceId, props).get();
    new RebootDatabaseProvider(rds, instanceId).get();
    info(LOGGER, "provisioned database [%s] - [%s]", instanceId, getTime(sw));
    return instanceId;
  }

}
