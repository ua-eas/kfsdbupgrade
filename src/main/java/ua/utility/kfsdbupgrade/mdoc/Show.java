package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Ints.checkedCast;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getCountFormatter;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.simple.Logging.info;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

public final class Show {

  private static final Logger LOGGER = getLogger(Show.class);

  public static void showSelect(DatabaseMetric snapshot) {
    List<Object> args = newArrayList();
    args.add("select-->");
    args.addAll(getArgs(snapshot.getOverall().getSelect(), snapshot.getOverallWallTimeMicros()));
    args.addAll(getArgs(snapshot.getCurrent().getSelect(), snapshot.getCurrentSelectWallTimeMicros()));
    doLogging(args);
  }

  public static void showConvert(DatabaseMetric snapshot) {
    List<Object> args = newArrayList();
    args.add("convert->");
    args.addAll(getArgs(snapshot.getOverall().getConvert(), snapshot.getOverallWallTimeMicros()));
    args.addAll(getArgs(snapshot.getCurrent().getConvert(), snapshot.getCurrentConvertWallTimeMicros()));
    doLogging(args);
  }

  public static void showUpdate(DatabaseMetric snapshot) {
    List<Object> args = newArrayList();
    args.add("update-->");
    args.addAll(getArgs(snapshot.getOverall().getUpdate(), snapshot.getOverallWallTimeMicros()));
    args.addAll(getArgs(snapshot.getCurrent().getUpdate(), snapshot.getCurrentUpdateWallTimeMicros()));
    doLogging(args);
  }

  public static void show(DatabaseMetric snapshot) {
    if (snapshot.getOverall().getSelect().getCount() > 0) {
      showSelect(snapshot);
    }
    if (snapshot.getOverall().getConvert().getCount() > 0) {
      showConvert(snapshot);
    }
    if (snapshot.getOverall().getUpdate().getCount() > 0) {
      showUpdate(snapshot);
    }
  }

  private static void doLogging(List<Object> args) {
    info(LOGGER, "%s[%s %s docs/sec cpu:%s|%s] [%s %s docs/sec cpu:%s|%s]", args.toArray());
  }

  private static ImmutableList<Object> getArgs(DataMetric dm, long wallTimeMicros) {
    String count = getCount(checkedCast(dm.getCount()));
    String iops = getIops(dm, wallTimeMicros);
    String time = getTime(dm.getMicroseconds() / 1000);
    String wtime = getTime(wallTimeMicros / 1000);
    return ImmutableList.<Object>of(count, iops, time, wtime);
  }

  public static String getIops(DataMetric metric, long microseconds) {
    double millis = microseconds / 1000D;
    double seconds = millis / 1000;
    double iops = metric.getCount() / seconds;
    return getCountFormatter().format(iops);
  }

}
