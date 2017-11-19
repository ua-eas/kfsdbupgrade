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

  public RebootDatabaseProvider(AmazonRDS rds, String name) {
    this.rds = checkNotNull(rds);
    this.name = checkNotBlank(name, "name");
  }

  private final AmazonRDS rds;
  private final String name;

  public String get() {
    Stopwatch sw = createStarted();
    checkPresent(rds, name);
    reboot(rds, name);
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, name);
    WaitContext ctx = new WaitContext(getMillis("5s"), getMillis("15m"), getMillis("1m"));
    Predicate<Optional<DBInstance>> predicate = (db) -> db.isPresent() && db.get().getDBInstanceStatus().equals(STATUS_AVAILABLE);
    info(LOGGER, "waiting up to %s for [%s] to be rebooted", getTime(ctx.getTimeout(), ctx.getUnit()), name);
    Optional<DBInstance> instance = new Waiter<>(ctx, provider, predicate).get();
    info(LOGGER, "database=%s, status=%s [%s]", name, instance.get().getDBInstanceStatus(), getTime(sw));
    return name;
  }

  private void reboot(AmazonRDS rds, String name) {
    RebootDBInstanceRequest request = new RebootDBInstanceRequest();
    request.setDBInstanceIdentifier(name);
    rds.rebootDBInstance(request);
  }

}
