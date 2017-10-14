package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Stopwatch.createUnstarted;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

public final class DatabaseMetrics {

  private final MDocMetrics overall;
  private final MDocMetrics current;
  private final Stopwatch stopwatch;

  public DatabaseMetrics(boolean startTimer) {
    this.overall = new MDocMetrics();
    this.current = new MDocMetrics();
    this.stopwatch = createUnstarted();
    if (startTimer) {
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

  public synchronized void resetCurrent() {
    checkStarted();
    this.current.reset();
  }

  private Stopwatch checkStarted() {
    checkArgument(stopwatch.isRunning(), "not started");
    return stopwatch;
  }

  private final ListMultimap<Long, Long> selects = ArrayListMultimap.create();

  public synchronized Stopwatch select(long count, long bytes, long microseconds) {
    checkStarted();
    selects.put(currentTimeMillis(), microseconds);
    this.current.select(count, bytes, microseconds);
    this.overall.select(count, bytes, microseconds);
    return createStarted();
  }

  public synchronized Stopwatch update(long count, long bytes, long microseconds) {
    checkStarted();
    this.current.update(count, bytes, microseconds);
    this.overall.update(count, bytes, microseconds);
    return createStarted();
  }

  public synchronized Stopwatch convert(long count, long bytes, long microseconds) {
    checkStarted();
    this.current.convert(count, bytes, microseconds);
    this.overall.convert(count, bytes, microseconds);
    return createStarted();
  }

  public synchronized DatabaseMetric getSnapshot() {
    checkStarted();
    MDocMetric o = new MDocMetric(overall.getSelect(), overall.getUpdate(), overall.getConvert());
    MDocMetric c = new MDocMetric(current.getSelect(), current.getUpdate(), current.getConvert());
    return new DatabaseMetric(o, c, stopwatch.elapsed(MICROSECONDS));
  }

  public synchronized ImmutableListMultimap<Long, Long> getSelects() {
    return ImmutableListMultimap.copyOf(selects);
  }

}
