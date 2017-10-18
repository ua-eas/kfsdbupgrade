package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Stopwatch;

public final class DataMetric {

  public DataMetric(int count, long bytes, Stopwatch sw) {
    this(count, bytes, sw.elapsed(MILLISECONDS));
  }

  public DataMetric(int count, long bytes, long millis) {
    checkArgument(count >= 0, "count must be greater than or equal to zero");
    checkArgument(bytes >= 0, "bytes must be greater than or equal to zero");
    checkArgument(millis >= 0, "millis must be greater than or equal to zero");
    this.count = count;
    this.bytes = bytes;
    this.millis = millis;
  }

  private final int count;
  private final long bytes;
  private final long millis;

  public int getCount() {
    return count;
  }

  public long getBytes() {
    return bytes;
  }

  public long getMillis() {
    return millis;
  }

}
