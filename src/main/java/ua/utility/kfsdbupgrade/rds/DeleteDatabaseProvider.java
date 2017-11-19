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

  public DeleteDatabaseProvider(AmazonRDS rds, String name) {
    this(rds, name, new Properties());
  }

  public DeleteDatabaseProvider(AmazonRDS rds, String name, Properties props) {
    this.rds = checkNotNull(rds);
    this.name = checkNotBlank(name, "name");
    this.props = checkNotNull(props);
  }

  private final AmazonRDS rds;
  private final String name;
  private final Properties props;

  public String get() {
    Stopwatch sw = createStarted();
    if (isAbsent(rds, name)) {
      info(LOGGER, "[%s] does not exist - skipping delete", name);
      return name;
    }
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, name);
    if (isDeleteRequired(provider.get())) {
      delete(rds, name);
    }
    WaitContext ctx = new WaitContext(getMillis("5s"), getMillis("15m"), getMillis("1m"));
    info(LOGGER, "waiting up to %s for [%s] to be deleted", getTime(ctx.getTimeout(), ctx.getUnit()), name);
    new Waiter<>(getMillis("15m"), provider, not(db -> db.isPresent())).get();
    info(LOGGER, "database=%s, status=deleted [%s]", name, getTime(sw));
    return name;
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
