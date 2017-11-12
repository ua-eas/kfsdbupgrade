package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.md.base.Threads.sleep;
import static ua.utility.kfsdbupgrade.rds.Rds.isAbsent;

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
    if (isAbsent(rds, instanceId)) {
      info(LOGGER, "skipping delete -> database [%s] does not exist", instanceId);
      return sw.elapsed(MILLISECONDS);
    }
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, instanceId);
    Optional<DBInstance> currentDatabase = provider.get();
    if (doDelete(currentDatabase)) {
      delete(rds, instanceId);
    }
    Optional<DBInstance> previousDatabase = absent();
    while (currentDatabase.isPresent()) {
      String currentStatus = currentDatabase.get().getDBInstanceStatus();
      String previousStatus = previousDatabase.isPresent() ? previousDatabase.get().getDBInstanceStatus() : "n/a";
      if (!currentStatus.equals(previousStatus)) {
        info(LOGGER, "database=%s, status=%s [%s]", instanceId, currentStatus, getTime(sw));
      }
      previousDatabase = currentDatabase;
      currentDatabase = provider.get();
      sleep(1000);
    }
    info(LOGGER, "database=%s, status=deleted [%s]", instanceId, getTime(sw));
    return sw.elapsed(MILLISECONDS);
  }

  private void delete(AmazonRDS rds, String instanceId) {
    DeleteDBInstanceRequest delete = new DeleteDBInstanceRequest();
    delete.setDBInstanceIdentifier(instanceId);
    delete.setSkipFinalSnapshot(true);
    info(LOGGER, "deleting database=%s,", instanceId);
    rds.deleteDBInstance(delete);
  }

  private boolean doDelete(Optional<DBInstance> instance) {
    return instance.isPresent() && !instance.get().getDBInstanceStatus().equalsIgnoreCase("deleting");
  }

}
