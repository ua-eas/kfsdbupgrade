package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.rds.Rds.STATUS_AVAILABLE;
import static ua.utility.kfsdbupgrade.rds.Rds.checkPresent;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RebootDBInstanceRequest;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;

public final class RebootDatabaseProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(RebootDatabaseProvider.class);

  public RebootDatabaseProvider(AmazonRDS rds, String instanceId) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
  }

  private final AmazonRDS rds;
  private final String instanceId;

  public String get() {
    Stopwatch sw = createStarted();
    checkPresent(rds, instanceId);
    reboot(rds, instanceId);
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, instanceId);
    WaitContext ctx = new WaitContext(getMillis("5s"), getMillis("15m"));
    Predicate<Optional<DBInstance>> predicate = (db) -> db.isPresent() && db.get().getDBInstanceStatus().equals(STATUS_AVAILABLE);
    info(LOGGER, "waiting up to %s for [%s] to be rebooted", getTime(ctx.getTimeout(), ctx.getUnit()), instanceId);
    Optional<DBInstance> instance = new Waiter<>(ctx, provider, predicate).get();
    info(LOGGER, "database=%s, status=%s [%s]", instanceId, instance.get().getDBInstanceStatus(), getTime(sw));
    return instanceId;
  }

  private void reboot(AmazonRDS rds, String instanceId) {
    RebootDBInstanceRequest request = new RebootDBInstanceRequest();
    request.setDBInstanceIdentifier(instanceId);
    rds.rebootDBInstance(request);
  }

}
