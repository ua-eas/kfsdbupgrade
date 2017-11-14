package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

public final class WaitContext {

  public WaitContext(long timeout) {
    this(5 * 1000, timeout, 60 * 1000);
  }

  public WaitContext(long duration, long timeout) {
    this(duration, timeout, 60 * 1000);
  }

  public WaitContext(long duration, long timeout, long display) {
    this(duration, timeout, display, MILLISECONDS);
  }

  public WaitContext(long duration, long timeout, long display, TimeUnit unit) {
    checkArgument(duration > 0, "duration must be > 0");
    checkArgument(timeout > 0, "timeout must be > 0");
    checkArgument(display > 0, "display must be > 0");
    checkArgument(timeout >= duration, "timeout cannot be less than duration");
    this.duration = duration;
    this.timeout = timeout;
    this.display = display;
    this.unit = checkNotNull(unit);
  }

  private final long duration;
  private final long timeout;
  private final long display;
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

  public long getDisplay() {
    return display;
  }

}
