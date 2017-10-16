package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.base.Optional.of;
import static java.lang.Thread.currentThread;
import static org.apache.commons.lang3.StringUtils.left;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import javax.inject.Provider;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;

/**
 * Print a log entry that includes timestamp, process id, thread name
 */
public final class LogPrefixProvider implements Provider<String> {

  private static final String FORMAT = "yyyy-MM-dd HH:mm:ss.SSS zzz";
  private static final TimeZone TIMEZONE = TimeZone.getTimeZone("US/Arizona");
  private static final Integer PID = ProcessIdProvider.INSTANCE.get().orNull();
  private static final Splitter SPLITTER = Splitter.on('.').omitEmptyStrings().trimResults();
  private static final String ROOT = "root";

  public LogPrefixProvider(long timestamp, Class<?> type) {
    this(timestamp, of(type.getCanonicalName()), "INFO");
  }

  public LogPrefixProvider(long timestamp, Optional<String> name, String level) {
    this.timestamp = timestamp;
    this.name = name;
    this.level = level;
  }

  private final long timestamp;
  private final Optional<String> name;
  private final String level;

  @Override
  public String get() {
    // New instance of SimpleDateFormat every single time because it isn't threadsafe
    SimpleDateFormat formatter = new SimpleDateFormat(FORMAT);
    formatter.setTimeZone(TIMEZONE);

    // extract the thread logging this event
    Thread thread = currentThread();

    // setup some storage for creating the log statement
    StringBuilder sb = new StringBuilder();

    // print a timestamp with timezone
    sb.append(formatter.format(new Date(timestamp)));
    sb.append(" ");

    // DEBUG,INFO,WARN,ERROR etc
    sb.append(level);
    sb.append(" ");

    // the process id of the jvm
    sb.append(PID);

    // thread information
    sb.append(" [");
    // the id of the current thread
    sb.append(thread.getId());
    sb.append(":");
    // first 30 characters of the thread name
    sb.append(left(thread.getName(), 30));
    sb.append("] ");

    // abbreviated package + class name
    sb.append(shorten(name));
    sb.append(" : ");

    return sb.toString();

  }

  private String shorten(Optional<String> name) {

    // if it's null we are done, just return the magic value "root"
    if (name.isPresent()) {

      // split the package + class name into individual tokens
      Iterator<String> itr = SPLITTER.split(name.get()).iterator();

      // setup some storage
      StringBuilder sb = new StringBuilder();
      while (itr.hasNext()) {
        // token is either a package name or a class name
        String token = itr.next();
        if (itr.hasNext()) {
          // package name, just use the first letter
          sb.append(token.substring(0, 1));
          sb.append('.');
        } else {
          // display the full class name
          sb.append(token);
        }
      }
      // return the abbreviated package + class name
      return sb.toString();
    } else {
      return ROOT;
    }

  }

}
