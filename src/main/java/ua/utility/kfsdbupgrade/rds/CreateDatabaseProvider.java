package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.md.base.Props.parseBoolean;
import static ua.utility.kfsdbupgrade.rds.Rds.STATUS_AVAILABLE;
import static ua.utility.kfsdbupgrade.rds.Rds.checkAbsent;
import static ua.utility.kfsdbupgrade.rds.Rds.checkedName;
import static ua.utility.kfsdbupgrade.rds.Rds.checkedSid;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;
import com.amazonaws.services.rds.model.Tag;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class CreateDatabaseProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(CreateDatabaseProvider.class);

  public CreateDatabaseProvider(AmazonRDS rds, String name, String sid, String snapshotId, Properties props) {
    this.rds = checkNotNull(rds);
    this.name = checkedName(name);
    this.sid = checkedSid(sid);
    this.snapshotId = checkNotBlank(snapshotId, "snapshotId");
    this.props = checkNotNull(props);
  }

  private final AmazonRDS rds;
  private final String name;
  private final String sid;
  private final String snapshotId;
  private final Properties props;

  public String get() {
    Stopwatch sw = createStarted();
    long timeout = getMillis(props.getProperty("rds.create.timeout", "1h"));
    info(LOGGER, "creating database [%s] from snapshot [%s]", name, snapshotId);
    checkAbsent(rds, name);
    List<Tag> tags = getTags(new TagsProvider(name, props).get());
    create(rds, name, sid, snapshotId, tags);
    info(LOGGER, "database created [%s] - [%s]", name, getTime(sw));
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, name);
    info(LOGGER, "waiting up to %s for [%s] to become available", getTime(timeout), name);
    Predicate<Optional<DBInstance>> predicate = (db) -> db.isPresent() && db.get().getDBInstanceStatus().equals(STATUS_AVAILABLE);
    DBInstance instance = new Waiter<>(timeout, provider, predicate).get().get();
    info(LOGGER, "database [%s, status=%s] [%s]", instance.getDBInstanceIdentifier(), instance.getDBInstanceStatus(), getTime(sw));
    return instance.getDBInstanceIdentifier();
  }

  private void create(AmazonRDS rds, String name, String sid, String snapshotId, Iterable<Tag> tags) {
    RestoreDBInstanceFromDBSnapshotRequest request = new RestoreDBInstanceFromDBSnapshotRequest();
    request.setDBSnapshotIdentifier(snapshotId);
    request.setDBInstanceIdentifier(name);
    request.setDBName(sid);
    request.setDBInstanceClass(props.getProperty("rds.instance.class", "db.m4.xlarge"));
    request.setDBSubnetGroupName(props.getProperty("rds.subnet.group.name", "rds-private-subnet-group"));
    request.setMultiAZ(parseBoolean(props, "rds.multi.az", false));
    request.setAutoMinorVersionUpgrade(parseBoolean(props, "rds.auto.minor.version.upgrade", true));
    request.setPubliclyAccessible(parseBoolean(props, "rds.publicly.accessible", false));
    request.setLicenseModel(props.getProperty("rds.license.model", "bring-your-own-license"));
    request.setEngine(props.getProperty("rds.engine", "oracle-ee"));
    request.setOptionGroupName(props.getProperty("rds.option.group.name", "ua-oracle-ee-12-1"));
    request.setCopyTagsToSnapshot(parseBoolean(props, "rds.copy.tags.to.snapshot", true));
    request.setTags(newList(tags));
    rds.restoreDBInstanceFromDBSnapshot(request);
  }

  private ImmutableList<Tag> getTags(Map<String, Optional<String>> tags) {
    List<Tag> list = newArrayList();
    for (String tag : tags.keySet()) {
      list.add(new Tag().withKey(tag).withValue(tags.get(tag).orNull()));
    }
    return newList(list);
  }

}
