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

import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public final class Show implements Provider<Long> {

  private static final Logger LOGGER = getLogger(Show.class);

  public Show(DataMetrics overall, Stopwatch sw, DataMetrics current, Stopwatch timer, String label) {
    this.overall = overall;
    this.sw = sw;
    this.current = current;
    this.timer = timer;
    this.label = label;
  }

  private final DataMetrics overall;
  private final Stopwatch sw;
  private final DataMetrics current;
  private final Stopwatch timer;
  private final String label;

  public Long get() {
    List<String> args = Lists.newArrayList();
    args.addAll(getArgs(overall, sw));
    args.addAll(getArgs(current, timer));
    args.add(label);
    info(LOGGER, "%s, %s, %s, %s, [%s] %s, %s, %s, %s, [%s] %s", args.toArray());
    return sw.elapsed(MILLISECONDS);
  }

  private ImmutableList<String> getArgs(DataMetrics metrics, Stopwatch sw) {
    String c = getCount(checkedCast(metrics.getCount().getValue()));
    String s = getSize(metrics.getBytes().getValue());
    long elapsed = metrics.getElapsed().getValue() / 1000;
    String t = getThroughputInSeconds(elapsed, metrics.getCount().getValue(), "rows/sec");
    String r = getRate(elapsed, metrics.getBytes().getValue());
    return ImmutableList.of(c, s, t, r, getTime(sw));
  }

}
