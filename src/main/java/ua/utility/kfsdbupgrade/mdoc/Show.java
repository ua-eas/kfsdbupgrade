package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Ints.checkedCast;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getRate;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getSize;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

public final class Show {

  private static final Logger LOGGER = getLogger(Show.class);

  public static void showSelect(DatabaseMetric ss) {
    show("s", ss.getOverall().getSelect(), ss.getCurrent().getSelect(), ss.getWallTimeMicros());
  }

  public static void showUpdate(DatabaseMetric ss) {
    show("u", ss.getOverall().getUpdate(), ss.getCurrent().getUpdate(), ss.getWallTimeMicros());
  }

  public static void showConvert(DatabaseMetric ss) {
    show("c", ss.getOverall().getConvert(), ss.getCurrent().getConvert(), ss.getWallTimeMicros());
  }

  public static void show(String prefix, DataMetric overall, DataMetric current, long microseconds) {
    long millis = microseconds / 1000;
    List<Object> args = newArrayList();
    args.add(prefix);
    args.addAll(getArgs(overall));
    args.addAll(getArgs(current));
    args.add(getTime(millis));
    info(LOGGER, "%s[%s %s %s %s %s] [%s %s %s %s %s] %s", args.toArray());
  }

  private static ImmutableList<Object> getArgs(DataMetric metric) {
    List<Object> args = newArrayList();
    long millis = metric.getMicroseconds() / 1000;
    args.add(getCount(checkedCast(metric.getCount())));
    args.add(getSize(metric.getBytes()));
    args.add(getThroughputInSeconds(millis, metric.getCount(), "rows/sec"));
    args.add(getRate(millis, metric.getBytes()));
    args.add(getTime(millis));
    return newList(args);
  }

}
