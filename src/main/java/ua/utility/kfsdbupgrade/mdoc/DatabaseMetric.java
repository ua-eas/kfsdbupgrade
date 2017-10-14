package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class DatabaseMetric {

  public DatabaseMetric(MDocMetric overall, MDocMetric current, long wallTimeMicros) {
    checkArgument(wallTimeMicros >= 0, "wallTimeMicros must be zero or greater");
    this.overall = checkNotNull(overall);
    this.current = checkNotNull(current);
    this.wallTimeMicros = wallTimeMicros;
  }

  private final MDocMetric overall;
  private final MDocMetric current;
  private final long wallTimeMicros;

  public MDocMetric getOverall() {
    return overall;
  }

  public MDocMetric getCurrent() {
    return current;
  }

  public long getWallTimeMicros() {
    return wallTimeMicros;
  }

}
