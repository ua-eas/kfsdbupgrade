package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.hash;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ComparisonChain;

/**
 * Holds information related to a period of time.
 *
 * Stop is guaranteed to be greater than or equal to start.
 *
 * Elapsed is guaranteed to be the difference between stop and start (might be zero)
 * 
 * @author Jeff Caddel
 */
public final class TimedInterval implements Comparable<TimedInterval> {

  private final long start;
  private final long stop;
  private final long elapsed;
  private final int hash;

  private TimedInterval(Builder builder) {
    this.start = builder.start;
    this.stop = builder.stop;
    this.elapsed = builder.elapsed;
    this.hash = hash(start, stop, elapsed);
  }

  public static TimedInterval build(Stopwatch sw) {
    return build(sw.elapsed(MILLISECONDS));
  }

  public static TimedInterval build(long elapsed) {
    // get the current timestamp from the system clock and use it to calculate the start time
    long stop = currentTimeMillis();
    long start = stop - elapsed;
    return builder().withStart(start).withStop(stop).withElapsed(elapsed).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long start = 0;
    private long stop = -1;
    private long elapsed = -1;

    public Builder withStart(long start) {
      this.start = start;
      return this;
    }

    public Builder withStop(long stop) {
      this.stop = stop;
      return this;
    }

    public Builder withElapsed(long elapsed) {
      this.elapsed = elapsed;
      return this;
    }

    public TimedInterval build() {
      return validate(new TimedInterval(this));
    }

    private static TimedInterval validate(TimedInterval instance) {
      checkArgument(instance.stop >= instance.start, "stop must be >= start");
      checkArgument(instance.elapsed == (instance.stop - instance.start), " elapsed must exactly equal (stop - start)");
      return instance;
    }

  }

  public long getStart() {
    return start;
  }

  public long getStop() {
    return stop;
  }

  public long getElapsed() {
    return elapsed;
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("start", start).add("stop", stop).add("elapsed", elapsed).toString();
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public int compareTo(TimedInterval other) {
    ComparisonChain chain = ComparisonChain.start();
    chain = chain.compare(start, other.start);
    chain = chain.compare(stop, other.stop);
    chain = chain.compare(elapsed, other.elapsed);
    return chain.result();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    } else if (object == null || object.getClass() != getClass()) {
      return false;
    } else {
      TimedInterval other = (TimedInterval) object;
      return (hash == other.hash) && compareTo(other) == 0;
    }
  }

}
