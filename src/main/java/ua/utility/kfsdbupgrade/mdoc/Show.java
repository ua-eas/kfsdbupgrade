package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Ints.checkedCast;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class Show {

  private static final Logger LOGGER = getLogger(Show.class);

  public static void show(DataMetrics overall, DataMetrics current, Stopwatch total, Stopwatch last) {
    show(overall, current, total, last, "");
  }

  public static void show(DataMetrics overall, DataMetrics current, Stopwatch total, Stopwatch last, String label) {
    synchronized (overall) {
      synchronized (current) {
        List<Object> args = newArrayList();
        args.addAll(getArgs(overall, total));
        args.addAll(getArgs(current, last));
        args.add(label);
        info(LOGGER, "[%s, %s, %s, %s, %s] [%s, %s, %s, %s, %s] %s", args.toArray());
      }
    }
  }

  private static ImmutableList<Object> getArgs(DataMetrics metrics, Stopwatch wallTime) {
    List<Object> args = newArrayList();
    long millis = wallTime.elapsed(MILLISECONDS);
    args.add(getCount(checkedCast(metrics.getCount())));
    args.add(getSize(metrics.getBytes()));
    args.add(getThroughputInSeconds(millis, metrics.getCount(), "rows/sec"));
    args.add(getRate(millis, metrics.getBytes()));
    args.add(getTime(millis));
    return newList(args);
  }

}
