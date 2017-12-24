package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;

import javax.inject.Provider;

import com.google.common.base.Splitter;

public final class LogStatsProvider implements Provider<LogStats> {

  public LogStatsProvider(DbUpgrade dbu) {
    this.dbu = checkNotNull(dbu);
  }

  private final DbUpgrade dbu;

  public LogStats get() {
    long info = 0;
    long warn = 0;
    long error = 0;
    long other = 0;
    long critical = 0;
    for (JobResult result : dbu.getResults()) {
      for (String line : Splitter.on('\n').split(result.getOutput())) {
        LogLineType type = LogLineTypeFunction.INSTANCE.apply(line);
        Severity severity = new SeverityProvider(type, result.getJob(), line).get();
        switch (type) {
        case INFO:
          info++;
          break;
        case WARNING:
          warn++;
          break;
        case ERROR:
          if (severity == Severity.HIGH) {
            critical++;
          } else {
            error++;
          }
          break;
        case OTHER:
          other++;
          break;
        default:
          throw illegalState("%s is an unknown log line type", type);
        }
      }
    }
    LogStats.Builder builder = LogStats.builder();
    builder.withError(error);
    builder.withInfo(info);
    builder.withOther(other);
    builder.withWarn(warn);
    builder.withCritical(critical);
    return builder.build();
  }

}
