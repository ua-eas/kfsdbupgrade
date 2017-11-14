package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Stopwatch.createStarted;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.md.base.Props.parseBoolean;
import static ua.utility.kfsdbupgrade.rds.Rds.isAbsent;

import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;

public final class DeleteDatabaseProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(DeleteDatabaseProvider.class);

  public DeleteDatabaseProvider(AmazonRDS rds, String instanceId, Properties props) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
    this.props = checkNotNull(props);
  }

  private final AmazonRDS rds;
  private final String instanceId;
  private final Properties props;

  public String get() {
    Stopwatch sw = createStarted();
    if (isAbsent(rds, instanceId)) {
      info(LOGGER, "[%s] does not exist - skipping delete", instanceId);
      return instanceId;
    }
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, instanceId);
    if (isDeleteRequired(provider.get())) {
      delete(rds, instanceId);
    }
    WaitContext ctx = new WaitContext(getMillis("5ms"), getMillis("15m"));
    info(LOGGER, "waiting up to %s for [%s] to be deleted", getTime(ctx.getTimeout(), ctx.getUnit()), instanceId);
    new Waiter<>(ctx, provider, not(db -> db.isPresent())).get();
    info(LOGGER, "database=%s, status=deleted [%s]", instanceId, getTime(sw));
    return instanceId;
  }

  private void delete(AmazonRDS rds, String instanceId) {
    info(LOGGER, "deleting database [%s]", instanceId);
    DeleteDBInstanceRequest delete = new DeleteDBInstanceRequest();
    delete.setDBInstanceIdentifier(instanceId);
    delete.setSkipFinalSnapshot(parseBoolean(props, "rds.skip.final.snapshot", true));
    rds.deleteDBInstance(delete);
  }

  private boolean isDeleteRequired(Optional<DBInstance> instance) {
    return instance.isPresent() && !instance.get().getDBInstanceStatus().equalsIgnoreCase("deleting");
  }

}
