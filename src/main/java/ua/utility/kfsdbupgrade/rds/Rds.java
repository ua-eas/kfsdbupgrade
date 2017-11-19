package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Ascii.isLowerCase;
import static com.google.common.base.Ascii.isUpperCase;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang3.StringUtils.isAllLowerCase;
import static org.apache.commons.lang3.StringUtils.isAllUpperCase;

import java.util.Locale;

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

  public static String checkedSid(String sid) {
    checkArgument(isAllUpperCase(sid), "[%s] must be all upper case", sid);
    checkArgument(sid.length() < 8, "[%s] %s characters is too long, max is 8", sid, sid.length());
    checkArgument(sid.length() > 0, "[%s] sid cannot be blank", sid);
    checkArgument(isNormalized(sid, false), "[%s] sid must only contain letters and digits", sid);
    return sid;
  }

  public static String getNormalizedName(String name) {
    return normalize(name.toLowerCase(Locale.ENGLISH).trim(), true);
  }

  public static String getNormalizedSid(String sid) {
    return normalize(sid.toUpperCase(Locale.ENGLISH).trim(), false);
  }

  public static String checkedName(String name) {
    checkArgument(isAllLowerCase(name), "[%s] must be all lower case", name);
    checkArgument(!name.endsWith("-"), "[%s] name cannot end with a hyphen", name);
    checkArgument(!name.contains("--"), "[%s] name cannot contain consecutive hyphens", name);
    checkArgument(name.length() < 63, "[%s] %s characters is too long, max is 63", name, name.length());
    checkArgument(name.length() > 0, "[%s] name cannot be blank", name);
    checkArgument(isLowerCase(name.charAt(0)), "[%s] name must start with a letter", name);
    checkArgument(isNormalized(name, true), "[%s] name must only contain letters, digits, and hyphens", name);
    return name;
  }

  private static boolean isNormalized(String string, boolean hyphens) {
    for (char c : string.trim().toCharArray()) {
      if (isNotAllowed(c, hyphens)) {
        return false;
      }
    }
    return true;
  }

  public static String normalize(String string, boolean hyphens) {
    StringBuilder sb = new StringBuilder();
    for (char c : string.trim().toCharArray()) {
      if (isAllowed(c, hyphens)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static boolean isNotAllowed(char c, boolean hyphens) {
    return !isAllowed(c, hyphens);
  }

  private static boolean isAllowed(char c, boolean hyphens) {
    return isUpperCase(c) || isLowerCase(c) || isDigit(c) || (hyphens && c == '-');
  }

  private static boolean isDigit(char c) {
    return (c >= '0' && c <= '9');
  }

  public static String checkAbsent(AmazonRDS rds, String name) {
    checkState(isAbsent(rds, name), "database [%s] already exists", name);
    return name;
  }

  public static String checkPresent(AmazonRDS rds, String name) {
    checkState(isPresent(rds, name), "database [%s] does not exist", name);
    return name;
  }

  public static boolean isAbsent(AmazonRDS rds, String name) {
    return !isPresent(rds, name);
  }

  public static boolean isPresent(AmazonRDS rds, String name) {
    return new DatabaseInstanceProvider(rds, name).get().isPresent();
  }

}
