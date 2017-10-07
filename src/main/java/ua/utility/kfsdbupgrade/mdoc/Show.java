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

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class Show {

  private static final Logger LOGGER = getLogger(Show.class);

  public static void show(DataMetrics overall, DataMetrics current, Stopwatch timer, String label) {
    synchronized (overall) {
      synchronized (current) {
        List<Object> args = newArrayList();
        args.addAll(getArgs(overall));
        args.addAll(getArgs(current));
        args.add(getTime(timer));
        args.add(label);
        info(LOGGER, "[%s, %s, %s, %s] [%s, %s, %s, %s] %s %s", args);
      }
    }
  }

  private static ImmutableList<Object> getArgs(DataMetrics metrics) {
    List<Object> args = newArrayList();
    long millis = metrics.getMicroseconds() / 1000;
    args.add(getCount(checkedCast(metrics.getCount())));
    args.add(getSize(metrics.getBytes()));
    args.add(getThroughputInSeconds(millis, metrics.getCount(), "rows/sec"));
    args.add(getRate(millis, metrics.getBytes()));
    return newList(args);
  }

}
