package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Stopwatch.createUnstarted;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Stopwatch;

public final class MDocMetrics {

  public MDocMetrics() {
    this.select = new DataMetrics();
    this.update = new DataMetrics();
    this.convert = new DataMetrics();
    this.stopwatch = createUnstarted();
  }

  private final DataMetrics select;
  private final DataMetrics update;
  private final DataMetrics convert;
  private final Stopwatch stopwatch;

  public synchronized DataMetrics getSelect() {
    return select;
  }

  public synchronized DataMetrics getUpdate() {
    return update;
  }

  public synchronized DataMetrics getConvert() {
    return convert;
  }

  public synchronized long elapsed() {
    checkArgument(stopwatch.isRunning(), "not running");
    return stopwatch.elapsed(MILLISECONDS);
  }

  public synchronized void start() {
    checkArgument(!stopwatch.isRunning(), "already running");
    stopwatch.start();
  }

}
