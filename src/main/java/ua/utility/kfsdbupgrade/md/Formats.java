package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.text.NumberFormat;

import com.google.common.base.Stopwatch;

import ua.utility.kfsdbupgrade.mdoc.Size;

public final class Formats {

  private Formats() {
  }

  private static final double SECOND = 1000;
  private static final double MINUTE = 60 * SECOND;
  private static final double HOUR = 60 * MINUTE;
  private static final double DAY = 24 * HOUR;
  private static final double YEAR = 365 * DAY;

  public static Size getSizeEnum(double bytes) {
    bytes = Math.abs(bytes);
    if (bytes < Size.KB.getValue()) {
      return Size.BYTE;
    } else if (bytes < Size.MB.getValue()) {
      return Size.KB;
    } else if (bytes < Size.GB.getValue()) {
      return Size.MB;
    } else if (bytes < Size.TB.getValue()) {
      return Size.GB;
    } else if (bytes < Size.PB.getValue()) {
      return Size.TB;
    } else if (bytes < Size.EB.getValue()) {
      return Size.PB;
    } else {
      return Size.EB;
    }
  }

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

  public static String getSize(long bytes) {
    Size uom = getSizeEnum(bytes);
    StringBuilder sb = new StringBuilder();
    sb.append(getFormattedSize(bytes, uom));
    sb.append(uom.getSizeLabel());
    return sb.toString();
  }

  public static String getRate(long millis, long bytes) {
    double seconds = millis / SECOND;
    double bytesPerSecond = bytes / seconds;
    Size bandwidthLevel = getSizeEnum(bytesPerSecond);
    double transferRate = bytesPerSecond / bandwidthLevel.getValue();
    return getRateFormatter().format(transferRate) + " " + bandwidthLevel.getRateLabel();
  }

  public static String getFormattedSize(long bytes, Size size) {
    switch (size) {
    case BYTE:
      return bytes + "";
    case KB:
    case MB:
    case GB:
    default:
      return getSizeFormatter().format(bytes / (double) size.getValue());
    }
  }

  public static NumberFormat getCountFormatter() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(0);
    nf.setMinimumFractionDigits(0);
    return nf;
  }

  public static NumberFormat getTimeFormatter() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(false);
    nf.setMaximumFractionDigits(3);
    nf.setMinimumFractionDigits(3);
    return nf;
  }

  public static NumberFormat getSizeFormatter() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(false);
    nf.setMaximumFractionDigits(3);
    nf.setMinimumFractionDigits(3);
    return nf;
  }

  public static NumberFormat getRateFormatter() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(false);
    nf.setMaximumFractionDigits(3);
    nf.setMinimumFractionDigits(3);
    return nf;
  }

}
