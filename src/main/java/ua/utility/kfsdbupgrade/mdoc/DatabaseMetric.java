package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class DatabaseMetric {

  public DatabaseMetric(MDocMetric overall, MDocMetric current, long overallWallTimeMicros, long currentWallTimeMicros) {
    checkArgument(overallWallTimeMicros >= 0, "overallWallTimeMicros cannot be negative");
    checkArgument(currentWallTimeMicros >= 0, "currentWallTimeMicros cannot be negative");
    this.overall = checkNotNull(overall);
    this.current = checkNotNull(current);
    this.overallWallTimeMicros = overallWallTimeMicros;
    this.currentWallTimeMicros = currentWallTimeMicros;
  }

  private final MDocMetric overall;
  private final MDocMetric current;
  private final long overallWallTimeMicros;
  private final long currentWallTimeMicros;

  public MDocMetric getOverall() {
    return overall;
  }

  public MDocMetric getCurrent() {
    return current;
  }

  public long getOverallWallTimeMicros() {
    return overallWallTimeMicros;
  }

  public long getCurrentWallTimeMicros() {
    return currentWallTimeMicros;
  }

}
