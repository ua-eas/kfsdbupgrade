package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Stopwatch.createUnstarted;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

import com.google.common.base.Stopwatch;

public final class DatabaseMetrics {

  private final MDocMetrics overall;
  private final MDocMetrics current;
  private final Stopwatch stopwatch;

  public DatabaseMetrics(boolean startImmediately) {
    this.overall = new MDocMetrics();
    this.current = new MDocMetrics();
    this.stopwatch = createUnstarted();
    if (startImmediately) {
      start();
    }
  }

  public DatabaseMetrics() {
    this(false);
  }

  public synchronized void start() {
    checkArgument(!stopwatch.isRunning(), "already started");
    this.stopwatch.start();
  }

  public synchronized void resetCurrentSelect() {
    checkStarted();
    this.current.resetSelect();
  }

  public synchronized void resetCurrentUpdate() {
    checkStarted();
    this.current.resetUpdate();
  }

  public synchronized void resetCurrentConvert() {
    checkStarted();
    this.current.resetConvert();
  }

  private Stopwatch checkStarted() {
    checkArgument(stopwatch.isRunning(), "not started");
    return stopwatch;
  }

  public synchronized long select(long count, long bytes, long microseconds) {
    checkStarted();
    this.current.select(count, bytes, microseconds);
    return this.overall.select(count, bytes, microseconds);
  }

  public synchronized long update(long count, long bytes, long microseconds) {
    checkStarted();
    this.current.update(count, bytes, microseconds);
    return this.overall.update(count, bytes, microseconds);
  }

  public synchronized long convert(long count, long bytes, long microseconds) {
    checkStarted();
    this.current.convert(count, bytes, microseconds);
    return this.overall.convert(count, bytes, microseconds);
  }

  public synchronized DatabaseMetric getSnapshot() {
    checkStarted();
    MDocMetric o = new MDocMetric(overall.getSelect(), overall.getUpdate(), overall.getConvert());
    MDocMetric c = new MDocMetric(current.getSelect(), current.getUpdate(), current.getConvert());
    return new DatabaseMetric(o, c, stopwatch.elapsed(MICROSECONDS));
  }

}
