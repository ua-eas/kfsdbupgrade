package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkState;

import com.amazonaws.services.rds.AmazonRDS;

public final class Rds {

  private Rds() {
  }

  public static String STATUS_AVAIALABLE = "available";

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
