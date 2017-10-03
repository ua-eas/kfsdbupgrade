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

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;

public final class TouchRowsProgressProvider implements Provider<Long> {

  private static final Logger LOGGER = getLogger(TouchRowsProgressProvider.class);

  public TouchRowsProgressProvider(DataMetrics metrics, Stopwatch sw, String label) {
    this.metrics = metrics;
    this.sw = sw;
    this.label = label;
  }

  private final DataMetrics metrics;
  private final Stopwatch sw;
  private final String label;

  public Long get() {
    String c = getCount(checkedCast(metrics.getCount().getValue()));
    String s = getSize(metrics.getBytes().getValue());
    long elapsed = metrics.getElapsed().getValue() / 1000;
    String t = getThroughputInSeconds(elapsed, metrics.getCount().getValue(), "rows/sec");
    String r = getRate(elapsed, metrics.getBytes().getValue());
    info(LOGGER, "%s, %s, %s, %s, [%s] %s", c, s, t, r, getTime(sw), label);
    return sw.elapsed(MILLISECONDS);
  }

}