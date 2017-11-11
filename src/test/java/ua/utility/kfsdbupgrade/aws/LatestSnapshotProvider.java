package ua.utility.kfsdbupgrade.aws;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.reverse;
import static org.apache.log4j.Logger.getLogger;
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

public final class LatestSnapshotProvider implements Provider<String> {

  private static final Logger LOGGER = getLogger(LatestSnapshotProvider.class);

  public LatestSnapshotProvider(AmazonRDS rds, String instanceId, Predicate<String> snapshotIdFilter) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
    this.snapshotIdFilter = checkNotNull(snapshotIdFilter);
  }

  private final AmazonRDS rds;
  private final String instanceId;
  private final Predicate<String> snapshotIdFilter;

  public String get() {
    DescribeDBSnapshotsRequest request = new DescribeDBSnapshotsRequest();
    request.setDBInstanceIdentifier(instanceId);
    Predicate<DBSnapshot> predicate = new SnapshotIdPredicate(snapshotIdFilter);
    List<DBSnapshot> snapshots = rds.describeDBSnapshots(request).getDBSnapshots();
    List<DBSnapshot> filtered = reverse(sort(snapshots, predicate, SnapshotCreationTime.INSTANCE));
    checkState(filtered.size() > 0, "no snapshots found matching %s [%s]", instanceId, snapshotIdFilter);
    DBSnapshot snapshot = filtered.iterator().next();
    info(LOGGER, "located %s snapshots matching %s and %s", getCount(filtered.size()), instanceId, snapshotIdFilter);
    info(LOGGER, "'%s' created on %s is the most recent", snapshot.getDBSnapshotIdentifier(), snapshot.getSnapshotCreateTime());
    return snapshot.getDBSnapshotIdentifier();
  }

  private static class SnapshotIdPredicate implements Predicate<DBSnapshot> {

    public SnapshotIdPredicate(Predicate<String> snapshotIdFilter) {
      this.snapshotIdFilter = checkNotNull(snapshotIdFilter);
    }

    private final Predicate<String> snapshotIdFilter;

    public boolean apply(DBSnapshot input) {
      return snapshotIdFilter.apply(input.getDBSnapshotIdentifier());
    }

  }

  private enum SnapshotCreationTime implements Function<DBSnapshot, Long> {
    INSTANCE;

    public Long apply(DBSnapshot input) {
      return input.getSnapshotCreateTime().getTime();
    }

  }

}
