package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Stopwatch.createStarted;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;

public final class DeleteDatabaseProvider implements Provider<Long> {

  private static final Logger LOGGER = getLogger(DeleteDatabaseProvider.class);

  public DeleteDatabaseProvider(AmazonRDS rds, String instanceId) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
  }

  private final AmazonRDS rds;
  private final String instanceId;

  public Long get() {
    Stopwatch sw = createStarted();
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, instanceId);
    if (deleteRequired(provider.get())) {
      delete(rds, instanceId);
    }
    WaitContext ctx = new WaitContext(getMillis("5s"), getMillis("15m"));
    info(LOGGER, "waiting up to %s for %s to be fully deleted", getTime(ctx.getTimeout(), ctx.getUnit()), instanceId);
    new Waiter<>(ctx, provider, not(db -> db.isPresent())).get();
    info(LOGGER, "database=%s, status=deleted [%s]", instanceId, getTime(sw));
    return sw.elapsed(MILLISECONDS);
  }

  private void delete(AmazonRDS rds, String instanceId) {
    DeleteDBInstanceRequest delete = new DeleteDBInstanceRequest();
    delete.setDBInstanceIdentifier(instanceId);
    delete.setSkipFinalSnapshot(true);
    info(LOGGER, "deleting database [%s]", instanceId);
    rds.deleteDBInstance(delete);
  }

  private boolean deleteRequired(Optional<DBInstance> instance) {
    return instance.isPresent() && !instance.get().getDBInstanceStatus().equalsIgnoreCase("deleting");
  }

}
