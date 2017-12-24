package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class NotableLogEntry {

  private final int step;
  private final String job;
  private final long line;
  private final LogLineType type;
  private final String text;
  private final Severity severity;

  private NotableLogEntry(Builder builder) {
    this.step = builder.step;
    this.job = builder.job;
    this.type = builder.type;
    this.line = builder.line;
    this.text = builder.text;
    this.severity = builder.severity;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private int step;
    private String job;
    private LogLineType type;
    private long line = -1;
    private String text;
    private Severity severity = Severity.MEDIUM;

    public Builder withSeverity(Severity severity) {
      this.severity = severity;
      return this;
    }

    public Builder withStep(int step) {
      this.step = step;
      return this;
    }

    public Builder withJob(String job) {
      this.job = job;
      return this;
    }

    public Builder withType(LogLineType type) {
      this.type = type;
      return this;
    }

    public Builder withLine(long line) {
      this.line = line;
      return this;
    }

    public Builder withText(String text) {
      this.text = text;
      return this;
    }

    public NotableLogEntry build() {
      return validate(new NotableLogEntry(this));
    }

    private static NotableLogEntry validate(NotableLogEntry instance) {
      checkArgument(instance.step >= 1, "step must be >= 1");
      checkArgument(isNotBlank(instance.job), "step may not be blank");
      // checkArgument(isNotBlank(instance.text), "text may not be blank");
      checkNotNull(instance.type, "type may not be null");
      checkNotNull(instance.severity, "severity may not be null");
      checkArgument(instance.line >= 1, "line must be >= 1");
      return instance;
    }
  }

  public int getStep() {
    return step;
  }

  public String getJob() {
    return job;
  }

  public LogLineType getType() {
    return type;
  }

  public long getLine() {
    return line;
  }

  public String getText() {
    return text;
  }

  public Severity getSeverity() {
    return severity;
  }

}
