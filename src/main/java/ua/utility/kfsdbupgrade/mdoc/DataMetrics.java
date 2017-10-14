package ua.utility.kfsdbupgrade.mdoc;

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

  public synchronized long increment(long count, long bytes, Stopwatch sw) {
    return increment(count, bytes, sw.elapsed(MICROSECONDS));
  }

  public synchronized long increment(long count, long bytes, long microseconds) {
    this.elapsed.increment(microseconds);
    this.bytes.increment(bytes);
    this.count.increment(count);
    return this.count.getValue();
  }

}
