package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.rds.Rds.checkAbsent;

import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;
import com.amazonaws.services.rds.model.Tag;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;

public final class CreateDatabaseProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(CreateDatabaseProvider.class);

  public CreateDatabaseProvider(AmazonRDS rds, String instanceId, String snapshotId) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
    this.snapshotId = checkNotBlank(snapshotId, "snapshotId");
  }

  private final AmazonRDS rds;
  private final String instanceId;
  private final String snapshotId;

  public String get() {
    Stopwatch sw = createStarted();
    checkAbsent(rds, instanceId);
    info(LOGGER, "creating database [%s] from snapshot [%s]", instanceId, snapshotId);
    create(rds, instanceId, snapshotId);
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, instanceId);
    WaitContext ctx = new WaitContext(getMillis("5s"), getMillis("1h"));
    info(LOGGER, "waiting up to %s for [%s] to become available", getTime(ctx.getTimeout(), ctx.getUnit()), instanceId);
    Predicate<Optional<DBInstance>> predicate = (db) -> db.isPresent() && db.get().getDBInstanceStatus().equals("available");
    Optional<DBInstance> database = new Waiter<>(ctx, provider, predicate).get();
    info(LOGGER, "database=%s, status=%s [%s]", instanceId, database.get().getDBInstanceStatus(), getTime(sw));
    return database.get().getDBInstanceIdentifier();
  }

  private void create(AmazonRDS rds, String instanceId, String snapshotId) {
    RestoreDBInstanceFromDBSnapshotRequest request = new RestoreDBInstanceFromDBSnapshotRequest();
    request.setDBInstanceIdentifier(instanceId);
    request.setDBSnapshotIdentifier(snapshotId);
    request.setDBInstanceClass("db.m4.xlarge");
    request.setDBSubnetGroupName("rds-private-subnet-group");
    request.setMultiAZ(false);
    request.setAutoMinorVersionUpgrade(true);
    request.setLicenseModel("bring-your-own-license");
    request.setDBName(instanceId);
    request.setEngine("oracle-ee");
    request.setOptionGroupName("ua-oracle-ee-12-1");
    request.setCopyTagsToSnapshot(true);
    String uaf = "UAccess Financials";
    List<Tag> tags = newArrayList();
    tags.add(new Tag().withKey("service").withValue(uaf));
    tags.add(new Tag().withKey("name").withValue(instanceId));
    tags.add(new Tag().withKey("environment").withValue("dev"));
    tags.add(new Tag().withKey("createdby").withValue("jcaddel"));
    tags.add(new Tag().withKey("contactnetid").withValue("jcaddel"));
    tags.add(new Tag().withKey("accountnumber").withValue(uaf));
    tags.add(new Tag().withKey("subaccount").withValue(uaf));
    tags.add(new Tag().withKey("ticketnumber").withValue("UAF-6014"));
    tags.add(new Tag().withKey("resourcefunction").withValue(format("%s RDS instance for executing database upgrade scripts", instanceId)));
    request.setTags(tags);
    rds.restoreDBInstanceFromDBSnapshot(request);
  }

}
