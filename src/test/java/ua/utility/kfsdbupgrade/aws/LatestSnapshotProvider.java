package ua.utility.kfsdbupgrade.aws;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.reverse;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.date;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.md.base.Lists.sort;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;

import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public final class LatestSnapshotProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(LatestSnapshotProvider.class);

  public LatestSnapshotProvider(AmazonRDS rds, String instanceId, boolean automatedOnly) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
    this.automatedOnly = automatedOnly;
  }

  private final AmazonRDS rds;
  private final String instanceId;
  private final boolean automatedOnly;

  public String get() {
    DescribeDBSnapshotsRequest request = new DescribeDBSnapshotsRequest();
    request.setDBInstanceIdentifier(instanceId);
    Predicate<DBSnapshot> predicate = (automatedOnly) ? AutomatedSnapshotPredicate.INSTANCE : Predicates.<DBSnapshot>alwaysTrue();
    String log = (automatedOnly) ? String.format("instance:%s %s", instanceId, predicate) : "instance:" + instanceId;
    List<DBSnapshot> snapshots = rds.describeDBSnapshots(request).getDBSnapshots();
    List<DBSnapshot> filtered = reverse(sort(snapshots, predicate, SnapshotCreationTime.INSTANCE));
    checkState(filtered.size() > 0, "no snapshots found matching [%s]", log);
    DBSnapshot snapshot = filtered.iterator().next();
    info(LOGGER, "located %s snapshots matching [%s]", getCount(filtered.size()), log);
    info(LOGGER, "snapshot [%s] created on %s is the most recent", snapshot.getDBSnapshotIdentifier(), date(snapshot.getSnapshotCreateTime().getTime()));
    return snapshot.getDBSnapshotIdentifier();
  }

  private enum AutomatedSnapshotPredicate implements Predicate<DBSnapshot> {
    INSTANCE;

    private final String prefix = "rds:";

    public boolean apply(DBSnapshot input) {
      return input.getDBSnapshotIdentifier().startsWith(prefix);
    }

    @Override
    public String toString() {
      return "StartsWith(" + prefix + ")";
    }

  }

  private enum SnapshotCreationTime implements Function<DBSnapshot, Long> {
    INSTANCE;

    public Long apply(DBSnapshot input) {
      return input.getSnapshotCreateTime().getTime();
    }

  }

}
