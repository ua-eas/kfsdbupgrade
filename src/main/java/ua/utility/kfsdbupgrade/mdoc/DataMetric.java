package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;

public final class DataMetric {

  public DataMetric(long count, long bytes, long microseconds) {
    checkArgument(count >= 0, "count must be zero or greater");
    checkArgument(bytes >= 0, "bytes must be zero or greater");
    checkArgument(microseconds >= 0, "microseconds must be zero or greater");
    this.count = count;
    this.bytes = bytes;
    this.microseconds = microseconds;
  }

  private final long count;
  private final long bytes;
  private final long microseconds;

  public long getCount() {
    return count;
  }

  public long getBytes() {
    return bytes;
  }

  public long getMicroseconds() {
    return microseconds;
  }

}
