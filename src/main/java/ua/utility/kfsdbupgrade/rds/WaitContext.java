package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.md.base.Formats.getMillis;

import java.util.concurrent.TimeUnit;

public final class WaitContext {

  private static final long DEFAULT_PAUSE = getMillis("30s");
  private static final long DEFAULT_INTERVAL = getMillis("10s");
  private static final long DEFAULT_DISPLAY = getMillis("1m");

  public WaitContext(long timeout) {
    this(DEFAULT_PAUSE, DEFAULT_INTERVAL, timeout, DEFAULT_DISPLAY);
  }

  public WaitContext(long pause, long interval, long timeout, long display) {
    this(pause, interval, timeout, display, MILLISECONDS);
  }

  public WaitContext(long pause, long interval, long timeout, long display, TimeUnit unit) {
    checkArgument(pause >= 0, "pause must be >= 0");
    checkArgument(interval >= 0, "interval must be >= 0");
    checkArgument(timeout > 0, "timeout must be > 0");
    checkArgument(timeout >= pause, "timeout must be >= pause");
    checkArgument(timeout >= interval, "timeout must be >= interval");
    checkArgument(display >= 0, "display must be >= 0");
    this.unit = checkNotNull(unit);
    this.pause = pause;
    this.interval = interval;
    this.timeout = timeout;
    this.display = display;
  }

  private final long pause;
  private final long interval;
  private final long timeout;
  private final long display;
  private final TimeUnit unit;

  public long getInterval() {
    return interval;
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

  public long getPause() {
    return pause;
  }

}
