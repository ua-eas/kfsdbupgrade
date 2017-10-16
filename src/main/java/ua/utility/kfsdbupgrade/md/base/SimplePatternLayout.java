package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.base.Optional.fromNullable;
import static org.apache.commons.lang.StringUtils.trimToNull;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import com.google.common.base.Optional;
import com.google.common.base.StandardSystemProperty;

/**
 * Print a log entry that includes timestamp, process id, thread name
 */
public final class SimplePatternLayout extends PatternLayout {

  private static final String LINE_SEPARATOR = StandardSystemProperty.LINE_SEPARATOR.value();

  @Override
  public String format(LoggingEvent event) {

    // get the prefix
    long timestamp = event.getTimeStamp();
    Optional<String> name = fromNullable(trimToNull(event.getLoggerName()));
    String level = event.getLevel().toString();
    String prefix = new LogPrefixProvider(timestamp, name, level).get();

    // setup some storage for creating the log statement
    StringBuilder sb = new StringBuilder();
    sb.append(prefix);

    // the actual log message
    sb.append(event.getMessage());
    sb.append(" ");

    // print a new line
    sb.append(LINE_SEPARATOR);
    return sb.toString();
  }

}
