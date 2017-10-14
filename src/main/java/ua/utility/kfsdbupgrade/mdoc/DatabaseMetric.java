package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class DatabaseMetric {

  private final MDocMetric overall;
  private final MDocMetric current;
  private final long overallWallTimeMicros;
  private final long currentSelectWallTimeMicros;
  private final long currentConvertWallTimeMicros;
  private final long currentUpdateWallTimeMicros;

  private DatabaseMetric(Builder builder) {
    this.overall = builder.overall;
    this.current = builder.current;
    this.overallWallTimeMicros = builder.overallWallTimeMicros;
    this.currentSelectWallTimeMicros = builder.currentSelectWallTimeMicros;
    this.currentConvertWallTimeMicros = builder.currentConvertWallTimeMicros;
    this.currentUpdateWallTimeMicros = builder.currentUpdateWallTimeMicros;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private MDocMetric overall;
    private MDocMetric current;
    private long overallWallTimeMicros = -1;
    private long currentSelectWallTimeMicros = -1;
    private long currentConvertWallTimeMicros = -1;
    private long currentUpdateWallTimeMicros = -1;

    public Builder withOverall(MDocMetric overall) {
      this.overall = overall;
      return this;
    }

    public Builder withCurrent(MDocMetric current) {
      this.current = current;
      return this;
    }

    public Builder withOverallWallTimeMicros(long overallWallTimeMicros) {
      this.overallWallTimeMicros = overallWallTimeMicros;
      return this;
    }

    public Builder withCurrentSelectWallTimeMicros(long currentSelectWallTimeMicros) {
      this.currentSelectWallTimeMicros = currentSelectWallTimeMicros;
      return this;
    }

    public Builder withCurrentConvertWallTimeMicros(long currentConvertWallTimeMicros) {
      this.currentConvertWallTimeMicros = currentConvertWallTimeMicros;
      return this;
    }

    public Builder withCurrentUpdateWallTimeMicros(long currentUpdateWallTimeMicros) {
      this.currentUpdateWallTimeMicros = currentUpdateWallTimeMicros;
      return this;
    }

    public DatabaseMetric build() {
      return validate(new DatabaseMetric(this));
    }

    private static DatabaseMetric validate(DatabaseMetric instance) {
      checkNotNull(instance.overall, "overall may not be null");
      checkNotNull(instance.current, "current may not be null");
      checkArgument(instance.overallWallTimeMicros >= 0L, "overallWallTimeMicros should be set");
      checkArgument(instance.currentSelectWallTimeMicros >= 0L, "currentSelectWallTimeMicros should be set");
      checkArgument(instance.currentConvertWallTimeMicros >= 0L, "currentConvertWallTimeMicros should be set");
      checkArgument(instance.currentUpdateWallTimeMicros >= 0L, "currentUpdateWallTimeMicros should be set");
      return instance;
    }
  }

  public MDocMetric getOverall() {
    return overall;
  }

  public MDocMetric getCurrent() {
    return current;
  }

  public long getOverallWallTimeMicros() {
    return overallWallTimeMicros;
  }

  public long getCurrentSelectWallTimeMicros() {
    return currentSelectWallTimeMicros;
  }

  public long getCurrentConvertWallTimeMicros() {
    return currentConvertWallTimeMicros;
  }

  public long getCurrentUpdateWallTimeMicros() {
    return currentUpdateWallTimeMicros;
  }

}
