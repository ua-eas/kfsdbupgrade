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
import static ua.utility.kfsdbupgrade.rds.Rds.STATUS_DELETING;
import static ua.utility.kfsdbupgrade.rds.Rds.isAbsent;

import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
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
    DBInstance instance = provider.get().get();
    if (isDeleteRequired(instance)) {
      info(LOGGER, "deleting database [%s]", name);
      delete(rds, name);
    } else {
      info(LOGGER, "database [%s] is already in state [%s]", name, STATUS_DELETING);
    }
    long timeout = getMillis(props.getProperty("rds.delete.timeout", "15m"));
    info(LOGGER, "waiting up to %s for [%s] to be deleted", getTime(timeout), name);
    new Waiter<>(timeout, provider, not(db -> db.isPresent())).get();
    info(LOGGER, "database [%s, status=deleted] [%s]", name, getTime(sw));
    return name;
  }

  private void delete(AmazonRDS rds, String name) {
    DeleteDBInstanceRequest delete = new DeleteDBInstanceRequest();
    delete.setDBInstanceIdentifier(name);
    delete.setSkipFinalSnapshot(parseBoolean(props, "rds.skip.final.snapshot", true));
    rds.deleteDBInstance(delete);
  }

  private boolean isDeleteRequired(DBInstance instance) {
    return !instance.getDBInstanceStatus().equalsIgnoreCase(STATUS_DELETING);
  }

}
