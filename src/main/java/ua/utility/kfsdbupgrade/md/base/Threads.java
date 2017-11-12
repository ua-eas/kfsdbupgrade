package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;

import java.util.concurrent.TimeUnit;

public final class Threads {

  private Threads() {
  }

  public static long checkedSleep(long millis, long timeout, long elapsed) {
    return checkedSleep(millis, timeout, elapsed, MILLISECONDS);
  }

  public static long checkedSleep(long duration, long timeout, long elapsed, TimeUnit unit) {
    checkState(elapsed <= timeout, "timeout exceeded %s > %s", getTime(elapsed, unit), getTime(timeout, unit));
    long sleep = min(duration, timeout - elapsed);
    return sleep(sleep, unit);
  }

  public static long sleep(long millis) {
    return sleep(millis, MILLISECONDS);
  }

  public static long sleep(long duration, TimeUnit unit) {
    try {
      Thread.sleep(MILLISECONDS.convert(duration, unit));
      return duration;
    } catch (InterruptedException e) {
      throw illegalState(e);
    }
  }

}
