package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;
import static org.apache.commons.lang3.StringUtils.substring;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalArgument;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;

import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class Formats {

  private Formats() {
  }

  private static final double SECOND = 1000;
  private static final double MINUTE = 60 * SECOND;
  private static final double HOUR = 60 * MINUTE;
  private static final double DAY = 24 * HOUR;
  private static final double YEAR = 365 * DAY;
  private static final ImmutableList<String> TIME_TOKENS = of("ms", "s", "m", "h", "d", "y");
  private static final ImmutableList<Long> TIME_MULTIPLIERS = getTimeMultipliers();

  /**
   * Parse milliseconds from a time string that ends with a unit of measure. If no unit of measure is provided, milliseconds is assumed. Unit of measure is case insensitive.
   *
   * <pre>
   *   1   == 1 millisecond
   *   1ms == 1 millisecond
   *   1s  == 1 second ==           1000 milliseconds
   *   1m  == 1 minute ==         60,000 milliseconds
   *   1h  == 1 hour   ==      3,600,000 milliseconds
   *   1d  == 1 day    ==     86,400,000 milliseconds
   *   1y  == 1 year   == 31,536,000,000 milliseconds
   * </pre>
   */
  public static long getMillis(String time) {
    return getMillis(time, TIME_TOKENS, TIME_MULTIPLIERS);
  }

  /**
   * Parse milliseconds from a time string that ends with a unit of measure. If no unit of measure is provided, milliseconds is assumed. Unit of measure is case insensitive.
   *
   * <pre>
   *   1   == 1 millisecond
   *   1ms == 1 millisecond
   *   1s  == 1 second ==           1000 milliseconds
   *   1m  == 1 minute ==         60,000 milliseconds
   *   1h  == 1 hour   ==      3,600,000 milliseconds
   *   1d  == 1 day    ==     86,400,000 milliseconds
   *   1y  == 1 year   == 31,536,000,000 milliseconds
   * </pre>
   */
  public static int getMillisAsInt(String time) {
    Long millis = getMillis(time);
    if (millis <= Integer.MAX_VALUE) {
      return millis.intValue();
    } else {
      throw illegalArgument("[%s] converts to [%s]. maximum allowable integer value is [%s]", time, millis, Integer.MAX_VALUE);
    }
  }

  private static long getMillis(String time, List<String> tokens, List<Long> multipliers) {
    checkNotBlank(time, "time");
    checkArgument(tokens.size() == multipliers.size(), "size mismatch");
    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);
      long multiplier = multipliers.get(i);
      if (endsWithIgnoreCase(time, token)) {
        return getTimeValue(time, token, multiplier);
      }
    }
    // Assume milliseconds
    return getTimeValue(time, "", 1);
  }

  protected static long getTimeValue(String time, String suffix, long multiplier) {
    int len = StringUtils.length(time);
    String substring = substring(time, 0, len - suffix.length());
    Double value = new Double(substring);
    value = value * multiplier;
    return value.longValue();
  }

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

  public static String getTime(long elapsed, TimeUnit unit) {
    return getTime(MILLISECONDS.convert(elapsed, unit));
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

  private static final ImmutableList<Long> getTimeMultipliers() {
    List<Long> m = newArrayList();
    m.add(1L);
    m.add(new Double(SECOND).longValue());
    m.add(new Double(MINUTE).longValue());
    m.add(new Double(HOUR).longValue());
    m.add(new Double(DAY).longValue());
    m.add(new Double(YEAR).longValue());
    return copyOf(m);
  }

}
