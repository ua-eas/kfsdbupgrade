package ua.utility.kfsdbupgrade.aws;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;
import com.amazonaws.services.rds.model.Tag;

public final class RdsTest {

  private static final Logger LOGGER = getLogger(RdsTest.class);

  @Test
  public void test() {
    try {
      String id = "kfs36014";
      String branch = "UAF-6014";
      AmazonRDS rds = new AmazonRdsProvider("us-west-2").get();
      String snapshotId = new LatestSnapshotProvider(rds, "kfs3imp", true).get();
      checkAbsent(rds, id);
      RestoreDBInstanceFromDBSnapshotRequest request = new RestoreDBInstanceFromDBSnapshotRequest();
      request.setDBInstanceIdentifier(id);
      request.setDBSnapshotIdentifier(snapshotId);
      request.setDBInstanceClass("db.m4.xlarge");
      request.setDBSubnetGroupName("rds-private-subnet-group");
      request.setMultiAZ(false);
      request.setAutoMinorVersionUpgrade(true);
      request.setLicenseModel("bring-your-own-license");
      request.setDBName(id);
      request.setEngine("oracle-ee");
      request.setOptionGroupName("ua-oracle-ee-12-1");
      request.setCopyTagsToSnapshot(true);
      String uaf = "UAccess Financials";
      List<Tag> tags = newArrayList();
      tags.add(new Tag().withKey("service").withValue(uaf));
      tags.add(new Tag().withKey("name").withValue(id));
      tags.add(new Tag().withKey("environment").withValue("dev"));
      tags.add(new Tag().withKey("createdby").withValue("jcaddel"));
      tags.add(new Tag().withKey("contactnetid").withValue("jcaddel"));
      tags.add(new Tag().withKey("accountnumber").withValue(uaf));
      tags.add(new Tag().withKey("subaccount").withValue(uaf));
      tags.add(new Tag().withKey("ticketnumber").withValue("UAF-6014"));
      tags.add(new Tag().withKey("resourcefunction").withValue(format("%s RDS instance for executing database upgrade scripts using branch %s", id, branch)));
      request.setTags(tags);

      DBInstance db = rds.restoreDBInstanceFromDBSnapshot(request);
      info(LOGGER, "db=%s, status=%s", db.getDBInstanceIdentifier(), db.getDBInstanceStatus());
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private String checkAbsent(AmazonRDS rds, String instanceId) {
    checkState(isAbsent(rds, instanceId), "database [%s] already exists", instanceId);
    return instanceId;
  }

  private boolean isAbsent(AmazonRDS rds, String instanceId) {
    return !isPresent(rds, instanceId);
  }

  private boolean isPresent(AmazonRDS rds, String instanceId) {
    for (DBInstance instance : rds.describeDBInstances().getDBInstances()) {
      if (instanceId.equalsIgnoreCase(instance.getDBInstanceIdentifier())) {
        return true;
      }
    }
    return false;
  }

}
