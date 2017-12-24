package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.inject.Provider;

public final class SeverityProvider implements Provider<Severity> {

  public SeverityProvider(LogLineType type, String step, String line) {
    checkArgument(isNotBlank(step));
    this.line = checkNotNull(line);
    this.step = step;
    this.type = checkNotNull(type);
  }

  private final String line;
  private final String step;
  private final LogLineType type;

  public Severity get() {
    if (line.contains("DistributedCacheManagerDecorator")
        && line.contains("failed to execute distributed flush for serviceName {http://rice.kuali.org/kew/v2_0}kewCacheAdminService")) {
      return Severity.LOW;
    }
    if (type == LogLineType.ERROR) {
      return Severity.HIGH;
    }
    if (line.contains("define") && line.contains("overlappping classes:")) {
      return Severity.LOW;
    }
    if (line.contains("[WARNING]   - ")) {
      return Severity.LOW;
    }
    if (line.contains("is using schema version dbchangelog rather than version 1.9")) {
      return Severity.LOW;
    }
    if (line.contains("JAXBConfigImpl") && line.contains("is not available and hence set to empty")) {
      return Severity.LOW;
    }
    return Severity.MEDIUM;
  }

  public String getLine() {
    return line;
  }

  public String getStep() {
    return step;
  }

  public LogLineType getType() {
    return type;
  }

}
