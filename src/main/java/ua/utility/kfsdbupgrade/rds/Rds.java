package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Ascii.isLowerCase;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Locale.ENGLISH;

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
    String normalized = normalize(sid, false).toUpperCase(ENGLISH);
    checkArgument(normalized.length() < 8, "[%s] sid must be 8 characters or less", normalized);
    return normalized;
  }

  public static String checkedName(String name) {
    String normalized = normalize(name, true);
    checkArgument(!normalized.endsWith("-"), "[%s] name cannot end with a hyphen", normalized);
    checkArgument(!normalized.contains("--"), "[%s] name cannot contain consecutive hyphens", normalized);
    checkArgument(normalized.length() < 63, "[%s] name must be 63 characters or less", normalized);
    checkArgument(normalized.length() > 0, "[%s] name must contain at least one character", normalized);
    checkArgument(isLowerCase(normalized.charAt(0)), "[%s] name must start with a letter", normalized);
    return normalized;
  }

  private static String normalize(String name, boolean hyphens) {
    String lower = name.toLowerCase(ENGLISH).trim();
    StringBuilder sb = new StringBuilder();
    for (char c : lower.toCharArray()) {
      if (isAllowed(c, hyphens)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static boolean isAllowed(char c, boolean hyphens) {
    return isLowerCase(c) || isDigit(c) || (hyphens && c == '-');
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
