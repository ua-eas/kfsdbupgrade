package ua.utility.kfsdbupgrade.log;

import static com.google.common.base.Optional.of;
import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.Thread.currentThread;
import static org.apache.commons.lang3.StringUtils.left;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Provider;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;

/**
 * Print a log entry that includes timestamp, process id, thread name
 */
public final class LogPrefixProvider implements Provider<String> {

  private static final String FORMAT = "yyyy-MM-dd HH:mm:ss.SSS zzz";
  private static final String TIMEZONE = "US/Arizona";
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
    formatter.setTimeZone(TimeZone.getTimeZone(TIMEZONE));

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
    sb.append(shorten(name.orNull()));
    sb.append(" : ");

    return sb.toString();

  }

  protected String shorten(String name) {

    // if it's null we are done, just return the magic value "root"
    if (name == null) {
      return ROOT;
    }

    // split the package + class name into individual tokens
    List<String> tokens = copyOf(SPLITTER.split(name));

    // setup some storage
    StringBuilder sb = new StringBuilder();

    // iterate over the package name tokens (do NOT process the class name)
    for (int i = 0; i < tokens.size() - 1; i++) {

      // shorten each token in the package name to one letter
      sb.append(tokens.get(i).substring(0, 1));

      // append a dot
      sb.append('.');
    }

    // append the class name
    sb.append(tokens.get(tokens.size() - 1));

    // return the abbreviated package + class name
    return sb.toString();
  }

}
