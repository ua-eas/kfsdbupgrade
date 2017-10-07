package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Stopwatch.createStarted;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

import com.google.common.base.Stopwatch;

public final class DataMetrics {

  public DataMetrics() {
    this.count = new Counter();
    this.bytes = new Counter();
    this.elapsed = new Counter();
  }

  private final Counter count;
  private final Counter bytes;
  private final Counter elapsed;

  public synchronized long getCount() {
    return count.getValue();
  }

  public synchronized long getBytes() {
    return bytes.getValue();
  }

  public synchronized long getMicroseconds() {
    return elapsed.getValue();
  }

  public synchronized Stopwatch elapsed(Stopwatch sw) {
    return increment(0, 0, sw);
  }

  public synchronized Stopwatch increment(long bytes, Stopwatch sw) {
    return increment(1, bytes, sw);
  }

  public synchronized Stopwatch increment(DataMetrics metrics) {
    return increment(metrics.getCount(), metrics.getBytes(), metrics.getMicroseconds());
  }

  public synchronized Stopwatch increment(long count, long bytes, Stopwatch sw) {
    return increment(count, bytes, sw.elapsed(MICROSECONDS));
  }

  private synchronized Stopwatch increment(long count, long bytes, long microseconds) {
    this.elapsed.increment(microseconds);
    this.count.increment(count);
    this.bytes.increment(bytes);
    return createStarted();
  }

}
