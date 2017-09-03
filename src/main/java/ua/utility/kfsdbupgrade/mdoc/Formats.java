package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.text.NumberFormat;

import com.google.common.base.Stopwatch;

public final class Formats {

  private Formats() {
  }

  private static final double SECOND = 1000;
  private static final double MINUTE = 60 * SECOND;
  private static final double HOUR = 60 * MINUTE;
  private static final double DAY = 24 * HOUR;
  private static final double YEAR = 365 * DAY;

  public static String getThroughputInSeconds(Stopwatch sw, long count, String label) {
    return getThroughputInSeconds(sw.elapsed(MILLISECONDS), count, label);
  }

  public static String getThroughputInSeconds(long millis, long count, String label) {
    double seconds = millis / SECOND;
    double countPerSecond = count / seconds;
    return getCountFormatter().format(countPerSecond) + " " + label;
  }

  public static String getCount(int count) {
    return getCountFormatter().format(count);
  }

  public static String getTime(Stopwatch sw) {
    return getTime(sw.elapsed(MILLISECONDS));
  }

  public static String getTime(long elapsed) {
    checkArgument(elapsed >= 0, "elapsed can't be negative");
    NumberFormat nf = getTimeFormatter();
    if (elapsed < SECOND) {
      return elapsed + "ms";
    } else if (elapsed < MINUTE) {
      return nf.format(elapsed / SECOND) + "s";
    } else if (elapsed < HOUR) {
      return nf.format(elapsed / MINUTE) + "m";
    } else if (elapsed < DAY) {
      return nf.format(elapsed / HOUR) + "h";
    } else if (elapsed < YEAR) {
      return nf.format(elapsed / DAY) + "d";
    } else {
      return nf.format(elapsed / YEAR) + "y";
    }
  }

  private static NumberFormat getCountFormatter() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(0);
    nf.setMinimumFractionDigits(0);
    return nf;
  }

  private static NumberFormat getTimeFormatter() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(false);
    nf.setMaximumFractionDigits(3);
    nf.setMinimumFractionDigits(3);
    return nf;
  }

}
