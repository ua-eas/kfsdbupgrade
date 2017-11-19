package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.util.Arrays.asList;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Props.checkedValue;
import static ua.utility.kfsdbupgrade.md.base.Props.parseBoolean;
import static ua.utility.kfsdbupgrade.rds.Rds.DEFAULT_AWS_REGION;
import static ua.utility.kfsdbupgrade.rds.Rds.DEFAULT_ORACLE_SID;
import static ua.utility.kfsdbupgrade.rds.Rds.checkPresent;

import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.google.common.base.Stopwatch;

public final class OracleDatabaseProvider implements Provider<OracleDatabase> {

  private static final Logger LOGGER = getLogger(OracleDatabaseProvider.class);

  public OracleDatabaseProvider(Properties props) {
    this.props = checkNotNull(props);
  }

  private final Properties props;

  @Override
  public OracleDatabase get() {
    Stopwatch sw = createStarted();
    String region = checkedValue(props, asList("aws.region", "AWS_DEFAULT_REGION"), DEFAULT_AWS_REGION);
    String snapshotName = checkedValue(props, "db.snapshot.name");
    String name = checkedValue(props, "db.name");
    String sid = props.getProperty("db.sid", DEFAULT_ORACLE_SID);
    AWSCredentials credentials = new CredentialsProvider(props).get();
    AmazonRDS rds = new AmazonRdsProvider(region, credentials).get();
    if (parseBoolean(props, "db.create", false)) {
      info(LOGGER, "provisioning new database");
      boolean automatedOnly = parseBoolean(props, "rds.snapshot.automated.only", true);
      String snapshotId = new LatestSnapshotProvider(rds, snapshotName, automatedOnly).get();
      new DeleteDatabaseProvider(rds, name, props).get();
      new CreateDatabaseProvider(rds, name, sid, snapshotId, props).get();
      new FinalizeDatabaseProvider(rds, name, props).get();
      new RebootDatabaseProvider(rds, name, props).get();
      info(LOGGER, "provisioned database [%s] - [%s]", name, getTime(sw));
    } else {
      checkPresent(rds, name);
    }
    DBInstance aws = new DatabaseInstanceProvider(rds, name).get().get();
    return OracleDatabaseFunction.INSTANCE.apply(aws);
  }

}
