package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

public final class WaitContext {

  public WaitContext(long duration, long timeout) {
    this(duration, timeout, MILLISECONDS);
  }

  public WaitContext(long duration, long timeout, TimeUnit unit) {
    checkArgument(duration > 0, "duration must be > 0");
    checkArgument(timeout > 0, "timeout must be > 0");
    checkArgument(timeout >= duration, "timeout cannot be less than duration");
    this.duration = duration;
    this.timeout = timeout;
    this.unit = checkNotNull(unit);
  }

  private final long duration;
  private final long timeout;
  private final TimeUnit unit;

  public long getDuration() {
    return duration;
  }

  public long getTimeout() {
    return timeout;
  }

  public TimeUnit getUnit() {
    return unit;
  }

}
