package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.md.base.Props.parseBoolean;
import static ua.utility.kfsdbupgrade.md.base.Props.parseInt;
import static ua.utility.kfsdbupgrade.md.base.Splitters.csv;
import static ua.utility.kfsdbupgrade.rds.Rds.STATUS_AVAILABLE;
import static ua.utility.kfsdbupgrade.rds.Rds.checkPresent;

import java.util.List;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;

public final class FinalizeDatabaseProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(FinalizeDatabaseProvider.class);

  public FinalizeDatabaseProvider(AmazonRDS rds, String instanceId, Properties props) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
    this.props = checkNotNull(props);
  }

  private final AmazonRDS rds;
  private final String instanceId;
  private final Properties props;

  public String get() {
    Stopwatch sw = createStarted();
    checkPresent(rds, instanceId);
    finalize(rds, instanceId);
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, instanceId);
    WaitContext ctx = new WaitContext(getMillis("5s"), getMillis("15m"));
    Predicate<Optional<DBInstance>> predicate = (db) -> db.isPresent() && db.get().getDBInstanceStatus().equals(STATUS_AVAILABLE);
    info(LOGGER, "waiting up to %s for [%s] to be modified", getTime(ctx.getTimeout(), ctx.getUnit()), instanceId);
    Optional<DBInstance> instance = new Waiter<>(ctx, provider, predicate).get();
    info(LOGGER, "database=%s, status=%s [%s]", instanceId, instance.get().getDBInstanceStatus(), getTime(sw));
    return instanceId;
  }

  private void finalize(AmazonRDS rds, String instanceId) {
    info(LOGGER, "modifying [%s]", instanceId);
    List<String> vpcSecurityGroupIds = csv(props.getProperty("rds.vpc.security.group.ids", "sg-9afa41e2"));
    ModifyDBInstanceRequest request = new ModifyDBInstanceRequest();
    request.setDBInstanceIdentifier(instanceId);
    request.setVpcSecurityGroupIds(vpcSecurityGroupIds);
    request.setDBParameterGroupName(props.getProperty("rds.parameter.group.name", "kuali-oracle-12-1"));
    request.setBackupRetentionPeriod(parseInt(props, "rds.backup.retention.period", 0));
    request.setApplyImmediately(parseBoolean(props, "rds.apply.immediately", true));
    rds.modifyDBInstance(request);
  }

}
