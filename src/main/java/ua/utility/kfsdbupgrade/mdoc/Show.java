package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.primitives.Ints.checkedCast;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getRate;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getSize;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;

public final class Show {

  private static final Logger LOGGER = getLogger(Show.class);

  public static void show(DataMetrics metrics, Stopwatch sw, String label) {
    synchronized (metrics) {
      long elapsed = sw.elapsed(MILLISECONDS);
      String c = getCount(checkedCast(metrics.getCount()));
      String s = getSize(metrics.getBytes());
      String t = getThroughputInSeconds(elapsed, metrics.getCount(), "rows/sec");
      String r = getRate(elapsed, metrics.getBytes());
      Object[] args = { c, s, t, r, getTime(elapsed), label };
      info(LOGGER, "%s, %s, %s, %s, [%s] %s", args);
    }
  }

}
