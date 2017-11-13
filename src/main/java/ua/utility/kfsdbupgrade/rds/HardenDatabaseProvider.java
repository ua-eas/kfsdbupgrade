package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.util.Arrays.asList;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.rds.Rds.STATUS_AVAILABLE;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;

public final class HardenDatabaseProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(HardenDatabaseProvider.class);

  public HardenDatabaseProvider(AmazonRDS rds, String instanceId) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
  }

  private final AmazonRDS rds;
  private final String instanceId;

  public String get() {
    Stopwatch sw = createStarted();
    Rds.checkPresent(rds, instanceId);
    harden(rds, instanceId);
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, instanceId);
    WaitContext ctx = new WaitContext(getMillis("5s"), getMillis("15m"));
    info(LOGGER, "waiting up to %s for [%s] to be hardened", getTime(ctx.getTimeout(), ctx.getUnit()), instanceId);
    Predicate<Optional<DBInstance>> predicate = (db) -> db.isPresent() && db.get().getDBInstanceStatus().equals(STATUS_AVAILABLE);
    Optional<DBInstance> instance = new Waiter<>(ctx, provider, predicate).get();
    info(LOGGER, "database=%s, status=%s [%s]", instanceId, instance.get().getDBInstanceStatus(), getTime(sw));
    return instanceId;
  }

  private void harden(AmazonRDS rds, String instanceId) {
    ModifyDBInstanceRequest request = new ModifyDBInstanceRequest();
    request.setDBInstanceIdentifier(instanceId);
    request.setVpcSecurityGroupIds(asList("sg-9afa41e2"));
    request.setDBParameterGroupName("kuali-oracle-12-1");
    request.setBackupRetentionPeriod(0);
    request.setApplyImmediately(true);
    rds.modifyDBInstance(request);
  }

}
