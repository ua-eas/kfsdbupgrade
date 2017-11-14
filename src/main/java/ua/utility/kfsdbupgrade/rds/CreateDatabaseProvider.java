package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
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

  public CreateDatabaseProvider(AmazonRDS rds, String instanceId, String snapshotId, Properties props) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
    this.snapshotId = checkNotBlank(snapshotId, "snapshotId");
    this.props = checkNotNull(props);
  }

  private final AmazonRDS rds;
  private final String instanceId;
  private final String snapshotId;
  private final Properties props;

  public String get() {
    Stopwatch sw = createStarted();
    info(LOGGER, "creating database [%s] from snapshot [%s]", instanceId, snapshotId);
    checkAbsent(rds, instanceId);
    List<Tag> tags = getTags(getDefaultTags(props, instanceId));
    create(rds, instanceId, snapshotId, tags);
    info(LOGGER, "database created [%s] - [%s]", instanceId, getTime(sw));
    DatabaseInstanceProvider provider = new DatabaseInstanceProvider(rds, instanceId);
    WaitContext ctx = new WaitContext(getMillis("5s"), getMillis("1h"), getMillis("1m"));
    info(LOGGER, "waiting up to %s for [%s] to become available", getTime(ctx.getTimeout(), ctx.getUnit()), instanceId);
    Predicate<Optional<DBInstance>> predicate = (db) -> db.isPresent() && db.get().getDBInstanceStatus().equals(STATUS_AVAILABLE);
    Optional<DBInstance> database = new Waiter<>(ctx, provider, predicate).get();
    info(LOGGER, "database=%s, status=%s [%s]", instanceId, database.get().getDBInstanceStatus(), getTime(sw));
    return database.get().getDBInstanceIdentifier();
  }

  private void create(AmazonRDS rds, String instanceId, String snapshotId, Iterable<Tag> tags) {
    RestoreDBInstanceFromDBSnapshotRequest request = new RestoreDBInstanceFromDBSnapshotRequest();
    request.setDBSnapshotIdentifier(snapshotId);
    request.setDBInstanceIdentifier(instanceId);
    request.setDBName(instanceId);
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

  private ImmutableMap<String, Optional<String>> getDefaultTags(Properties props, String instanceId) {
    String uaf = props.getProperty("rds.account", "UAccess Financials");
    Map<String, Optional<String>> map = newLinkedHashMap();
    map.put("service", fromNullable(props.getProperty("rds.tag.service", uaf)));
    map.put("accountnumber", fromNullable(props.getProperty("rds.tag.accountnumber", uaf)));
    map.put("subaccount", fromNullable(props.getProperty("rds.tag.subaccount", uaf)));
    map.put("name", fromNullable(props.getProperty("rds.tag.name", instanceId)));
    map.put("environment", fromNullable(props.getProperty("rds.tag.environment", "dev")));
    String prefix = "rds.tag.";
    for (String key : filter(props.stringPropertyNames(), key -> key.startsWith(prefix))) {
      map.put(removeStart(key, prefix), fromNullable(props.getProperty(key)));
    }
    return ImmutableMap.copyOf(map);
  }

}
