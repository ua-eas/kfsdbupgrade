package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Stopwatch.createUnstarted;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Show.show;

import com.google.common.base.Stopwatch;

public final class DatabaseMetrics {

  private MDocMetrics overall;
  private MDocMetrics current;
  private long count;
  private final Stopwatch overallTimer;
  private final Stopwatch currentTimer;
  private final long show = 1000;

  public DatabaseMetrics(boolean startImmediately) {
    this.overall = new MDocMetrics();
    this.current = new MDocMetrics();
    this.overallTimer = createUnstarted();
    this.currentTimer = createUnstarted();
    this.count = 0;
    if (startImmediately) {
      start();
    }
  }

  public DatabaseMetrics() {
    this(false);
  }

  public synchronized void start() {
    checkArgument(!overallTimer.isRunning(), "already started");
    this.overallTimer.start();
    this.currentTimer.start();
  }

  private synchronized void resetCurrent() {
    checkStarted();
    this.current = new MDocMetrics();
    this.currentTimer.reset().start();
  }

  private Stopwatch checkStarted() {
    checkArgument(overallTimer.isRunning(), "not started");
    return overallTimer;
  }

  public synchronized void select(long bytes, long microseconds) {
    checkStarted();
    this.current.select(1, bytes, microseconds);
    this.overall.select(1, bytes, microseconds);
    this.count += 1;
    if (count % show == 0) {
      show(getSnapshot());
      resetCurrent();
    }
  }

  public synchronized void update(long bytes, long microseconds) {
    checkStarted();
    this.current.update(1, bytes, microseconds);
    this.overall.update(1, bytes, microseconds);
    this.count += 1;
    if (count % show == 0) {
      show(getSnapshot());
      resetCurrent();
    }
  }

  public synchronized void convert(long bytes, long microseconds) {
    checkStarted();
    this.current.convert(1, bytes, microseconds);
    this.overall.convert(1, bytes, microseconds);
    this.count += 1;
    if (count % show == 0) {
      show(getSnapshot());
      resetCurrent();
    }
  }

  public synchronized DatabaseMetric getSnapshot() {
    checkStarted();
    MDocMetric o = new MDocMetric(overall.getSelect(), overall.getUpdate(), overall.getConvert());
    MDocMetric c = new MDocMetric(current.getSelect(), current.getUpdate(), current.getConvert());
    return new DatabaseMetric(o, c, overallTimer.elapsed(MICROSECONDS), currentTimer.elapsed(MICROSECONDS));
  }

}
