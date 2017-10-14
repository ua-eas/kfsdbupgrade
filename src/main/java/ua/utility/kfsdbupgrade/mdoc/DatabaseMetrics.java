package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Stopwatch.createUnstarted;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Show.showConvert;
import static ua.utility.kfsdbupgrade.mdoc.Show.showSelect;
import static ua.utility.kfsdbupgrade.mdoc.Show.showUpdate;

import com.google.common.base.Stopwatch;

public final class DatabaseMetrics {

  private final MDocMetrics overall;
  private final Stopwatch overallTimer;
  private final Stopwatch currentSelectTimer;
  private final Stopwatch currentConvertTimer;
  private final Stopwatch currentUpdateTimer;
  private final long show;

  private MDocMetrics current;
  private long selectCount;
  private long convertCount;
  private long updateCount;

  public DatabaseMetrics(long show, boolean startImmediately) {
    this.overall = new MDocMetrics();
    this.current = new MDocMetrics();
    this.overallTimer = createUnstarted();
    this.currentSelectTimer = createUnstarted();
    this.currentConvertTimer = createUnstarted();
    this.currentUpdateTimer = createUnstarted();
    this.show = show;
    this.selectCount = 0;
    this.convertCount = 0;
    this.updateCount = 0;
    if (startImmediately) {
      start();
    }
  }

  public synchronized void start() {
    checkArgument(!overallTimer.isRunning(), "overall timer already started");
    checkArgument(!currentSelectTimer.isRunning(), "current select timer already started");
    checkArgument(!currentConvertTimer.isRunning(), "current convert timer already started");
    checkArgument(!currentUpdateTimer.isRunning(), "current update timer already started");
    this.overallTimer.start();
    this.currentSelectTimer.start();
    this.currentConvertTimer.start();
    this.currentUpdateTimer.start();
  }

  private synchronized void resetCurrentSelect() {
    checkStarted();
    this.current.resetSelect();
    this.currentSelectTimer.reset().start();
  }

  private synchronized void resetCurrentConvert() {
    checkStarted();
    this.current.resetConvert();
    this.currentConvertTimer.reset().start();
  }

  private synchronized void resetCurrentUpdate() {
    checkStarted();
    this.current.resetUpdate();
    this.currentUpdateTimer.reset().start();
  }

  private synchronized void checkStarted() {
    checkArgument(overallTimer.isRunning(), "overall timer not started");
    checkArgument(currentSelectTimer.isRunning(), "current select timer not started");
    checkArgument(currentConvertTimer.isRunning(), "current convert timer not started");
    checkArgument(currentUpdateTimer.isRunning(), "current update timer not started");
  }

  public synchronized void select(long bytes, long microseconds) {
    checkStarted();
    this.current.select(1, bytes, microseconds);
    this.overall.select(1, bytes, microseconds);
    this.selectCount += 1;
    if (selectCount % show == 0) {
      showSelect(getSnapshot());
      resetCurrentSelect();
    }
  }

  public synchronized void convert(long bytes, long microseconds) {
    checkStarted();
    this.current.convert(1, bytes, microseconds);
    this.overall.convert(1, bytes, microseconds);
    this.convertCount += 1;
    if (convertCount % show == 0) {
      showConvert(getSnapshot());
      resetCurrentConvert();
    }
  }

  public synchronized void update(long microseconds) {
    checkStarted();
    this.current.update(0, 0, microseconds);
    this.overall.update(0, 0, microseconds);
  }

  public synchronized void update(long bytes, long microseconds) {
    checkStarted();
    this.current.update(1, bytes, microseconds);
    this.overall.update(1, bytes, microseconds);
    this.updateCount += 1;
    if (updateCount % show == 0) {
      showUpdate(getSnapshot());
      resetCurrentUpdate();
    }
  }

  public synchronized DatabaseMetric getSnapshot() {
    checkStarted();
    MDocMetric o = new MDocMetric(overall.getSelect(), overall.getUpdate(), overall.getConvert());
    MDocMetric c = new MDocMetric(current.getSelect(), current.getUpdate(), current.getConvert());
    DatabaseMetric.Builder builder = DatabaseMetric.builder();
    builder.withOverall(o);
    builder.withOverallWallTimeMicros(overallTimer.elapsed(MICROSECONDS));
    builder.withCurrent(c);
    builder.withCurrentSelectWallTimeMicros(currentSelectTimer.elapsed(MICROSECONDS));
    builder.withCurrentConvertWallTimeMicros(currentConvertTimer.elapsed(MICROSECONDS));
    builder.withCurrentUpdateWallTimeMicros(currentUpdateTimer.elapsed(MICROSECONDS));
    return builder.build();
  }

}
