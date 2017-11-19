package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkState;

import com.amazonaws.services.rds.AmazonRDS;

public final class Rds {

  private Rds() {
  }

  public static final String STATUS_AVAILABLE = "available";
  public static final String STATUS_MODIFYING = "modifying";
  public static final String STATUS_DELETING = "deleting";
  public static final String DEFAULT_ORACLE_SID = "ORCL";
  public static final String DEFAULT_AWS_ACCOUNT = "UAccess Financials";
  public static final String DEFAULT_ENVIRONMENT = "dev";
  public static final String DEFAULT_AWS_REGION = "us-west-2";

  public static String checkAbsent(AmazonRDS rds, String instanceId) {
    checkState(isAbsent(rds, instanceId), "database [%s] already exists", instanceId);
    return instanceId;
  }

  public static String checkPresent(AmazonRDS rds, String instanceId) {
    checkState(isPresent(rds, instanceId), "database [%s] does not exist", instanceId);
    return instanceId;
  }

  public static boolean isAbsent(AmazonRDS rds, String instanceId) {
    return !isPresent(rds, instanceId);
  }

  public static boolean isPresent(AmazonRDS rds, String instanceId) {
    return new DatabaseInstanceProvider(rds, instanceId).get().isPresent();
  }

}
