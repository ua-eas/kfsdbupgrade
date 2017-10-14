package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Stopwatch.createUnstarted;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

import com.google.common.base.Stopwatch;

public final class MDocMetrics {

  public MDocMetrics() {
    this.select = new DataMetrics();
    this.update = new DataMetrics();
    this.convert = new DataMetrics();
    this.stopwatch = createUnstarted();
  }

  private final DataMetrics select;
  private final DataMetrics update;
  private final DataMetrics convert;
  private final Stopwatch stopwatch;

  public synchronized void start() {
    checkArgument(!stopwatch.isRunning(), "already running");
    stopwatch.start();
  }

  public synchronized long getMicroseconds() {
    checkArgument(stopwatch.isRunning(), "not running");
    return stopwatch.elapsed(MICROSECONDS);
  }

  public synchronized Stopwatch select(long count, long bytes, Stopwatch sw) {
    this.select.increment(count, bytes, sw);
    return createStarted();
  }

  public synchronized Stopwatch update(long count, long bytes, Stopwatch sw) {
    this.update.increment(count, bytes, sw);
    return createStarted();
  }

  public synchronized Stopwatch convert(long count, long bytes, Stopwatch sw) {
    this.convert.increment(count, bytes, sw);
    return createStarted();
  }

  public synchronized DataMetric getSelect() {
    return new DataMetric(select.getCount(), select.getBytes(), select.getMicroseconds());
  }

  public synchronized DataMetric getUpdate() {
    return new DataMetric(update.getCount(), update.getBytes(), update.getMicroseconds());
  }

  public synchronized DataMetric getConvert() {
    return new DataMetric(convert.getCount(), convert.getBytes(), convert.getMicroseconds());
  }

}
