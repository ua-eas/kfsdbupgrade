package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Lists.filter;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;
import static ua.utility.kfsdbupgrade.md.base.Lists.transform;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;
import static ua.utility.kfsdbupgrade.md.base.Props.parseBoolean;
import static ua.utility.kfsdbupgrade.md.base.Splitters.csv;
import static ua.utility.kfsdbupgrade.md.base.Splitters.split;
import static ua.utility.kfsdbupgrade.rds.Rds.DEFAULT_AWS_ACCOUNT;
import static ua.utility.kfsdbupgrade.rds.Rds.DEFAULT_ENVIRONMENT;
import static ua.utility.kfsdbupgrade.rds.Rds.STATUS_AVAILABLE;
import static ua.utility.kfsdbupgrade.rds.Rds.checkAbsent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;
import com.amazonaws.services.rds.model.Tag;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class CreateDatabaseProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(CreateDatabaseProvider.class);

  public CreateDatabaseProvider(AmazonRDS rds, String name, String sid, String snapshotId, Properties props) {
    this.rds = checkNotNull(rds);
    this.name = checkNotBlank(name, "name");
    this.sid = checkNotBlank(sid, "sid").toUpperCase(ENGLISH);
    this.snapshotId = checkNotBlank(snapshotId, "snapshotId");
    this.props = checkNotNull(props);
    checkArgument(sid.length() < 8, "max length for an oracle sid is 8 characters");
    checkArgument(name.length() < 63, "max length for an rds database name is 63 characters");
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
    List<Tag> tags = getTags(getDefaultTags(props, name));
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

  private ImmutableList<Tag> getTags(Map<String, Optional<String>> defaultTags) {
    List<String> strings = transform(defaultTags.entrySet(), e -> e.getKey() + (e.getValue().isPresent() ? "=" + e.getValue().get() : ""));
    String tags = props.getProperty("rds.tags", Joiner.on(',').join(strings));
    List<Tag> list = newArrayList();
    for (String tag : csv(tags)) {
      Iterator<String> itr = split('=', tag).iterator();
      list.add(new Tag().withKey(itr.next()).withValue(itr.hasNext() ? itr.next() : null));
    }
    return newList(list);
  }

  private ImmutableMap<String, Optional<String>> getDefaultTags(Properties props, String name) {
    String account = props.getProperty("rds.account", DEFAULT_AWS_ACCOUNT);
    Map<String, Optional<String>> map = newLinkedHashMap();
    map.put("service", fromNullable(props.getProperty("rds.tag.service", account)));
    map.put("accountnumber", fromNullable(props.getProperty("rds.tag.accountnumber", account)));
    map.put("subaccount", fromNullable(props.getProperty("rds.tag.subaccount", account)));
    map.put("name", fromNullable(props.getProperty("rds.tag.name", name)));
    map.put("environment", fromNullable(props.getProperty("rds.tag.environment", DEFAULT_ENVIRONMENT)));
    String prefix = "rds.tag.";
    for (String key : filter(props.stringPropertyNames(), key -> key.startsWith(prefix))) {
      map.put(removeStart(key, prefix), fromNullable(props.getProperty(key)));
    }
    return ImmutableMap.copyOf(map);
  }

}
