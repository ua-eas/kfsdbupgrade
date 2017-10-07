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

import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class Show implements Provider<Long> {

  private static final Logger LOGGER = getLogger(Show.class);

  public Show(DataMetrics overall, Stopwatch sw, String label) {
    this.overall = overall;
    this.sw = sw;
    this.label = label;
  }

  private final DataMetrics overall;
  private final Stopwatch sw;
  private final String label;

  public Long get() {
    List<String> args = newArrayList();
    args.addAll(getArgs(overall, sw));
    args.add(label);
    info(LOGGER, "%s, %s, %s, %s, [%s] %s", args.toArray());
    return sw.elapsed(MILLISECONDS);
  }

  private ImmutableList<String> getArgs(DataMetrics metrics, Stopwatch sw) {
    long elapsed = sw.elapsed(MILLISECONDS);
    String c = getCount(checkedCast(metrics.getCount()));
    String s = getSize(metrics.getBytes());
    String t = getThroughputInSeconds(elapsed, metrics.getCount(), "rows/sec");
    String r = getRate(elapsed, metrics.getBytes());
    return ImmutableList.of(c, s, t, r, getTime(elapsed));
  }

}
