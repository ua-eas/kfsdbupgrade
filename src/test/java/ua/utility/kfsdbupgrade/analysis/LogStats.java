package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;

public final class LogStats {

  private final long info;
  private final long warn;
  private final long error;
  private final long other;
  private final long critical;

  private LogStats(Builder builder) {
    this.info = builder.info;
    this.warn = builder.warn;
    this.error = builder.error;
    this.other = builder.other;
    this.critical = builder.critical;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long info = -1;
    private long warn = -1;
    private long error = -1;
    private long other = -1;
    private long critical = -1;

    public Builder withCritical(long critical) {
      this.critical = critical;
      return this;
    }

    public Builder withInfo(long info) {
      this.info = info;
      return this;
    }

    public Builder withWarn(long warn) {
      this.warn = warn;
      return this;
    }

    public Builder withError(long error) {
      this.error = error;
      return this;
    }

    public Builder withOther(long other) {
      this.other = other;
      return this;
    }

    public LogStats build() {
      return validate(new LogStats(this));
    }

    private static LogStats validate(LogStats instance) {
      checkArgument(instance.info >= 0L, "info should be set");
      checkArgument(instance.warn >= 0L, "warn should be set");
      checkArgument(instance.error >= 0L, "error should be set");
      checkArgument(instance.other >= 0L, "other should be set");
      checkArgument(instance.critical >= 0L, "critical should be set");
      return instance;
    }
  }

  public long getInfo() {
    return info;
  }

  public long getWarn() {
    return warn;
  }

  public long getError() {
    return error;
  }

  public long getOther() {
    return other;
  }

  public long getCritical() {
    return critical;
  }

}
