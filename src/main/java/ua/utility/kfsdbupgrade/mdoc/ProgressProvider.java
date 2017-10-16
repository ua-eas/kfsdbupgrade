package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.md.Formats.getCount;
import static ua.utility.kfsdbupgrade.md.Formats.getRate;
import static ua.utility.kfsdbupgrade.md.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.md.Formats.getTime;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;

public final class ProgressProvider implements Provider<Long> {

  private static final Logger LOGGER = getLogger(ProgressProvider.class);

  public ProgressProvider(MDocMetrics metrics, String label) {
    this(metrics, Optional.of(label));
  }

  public ProgressProvider(MDocMetrics metrics) {
    this(metrics, Optional.<String>absent());
  }

  private ProgressProvider(MDocMetrics metrics, Optional<String> label) {
    this.metrics = metrics;
    this.label = label;
  }

  private final MDocMetrics metrics;
  private final Optional<String> label;

  public Long get() {
    int count = checkedCast(metrics.getUpdate().getCount());
    long milliseconds = 0L; // metrics.getMicroseconds() / 1000;
    String dps = getThroughputInSeconds(milliseconds, count, "docs/second");
    String select = getTime(metrics.getSelect().getMicroseconds() / 1000);
    String update = getTime(metrics.getUpdate().getMicroseconds() / 1000);
    String convert = getTime(metrics.getConvert().getMicroseconds() / 1000);
    String rate = getRate(milliseconds, metrics.getSelect().getBytes() + metrics.getUpdate().getBytes());
    info("%s %s %s, %s [s:%s u:%s c:%s] %s", getCount(count), getTime(milliseconds), dps, rate, select, update, convert, label.or(""));
    return 0L;
  }

  private void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

}
