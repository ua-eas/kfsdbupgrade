package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.math.DoubleMath.roundToInt;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;

import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

public final class ThreadsProvider implements Provider<Integer> {

  private static final Logger LOGGER = getLogger(ThreadsProvider.class);

  private static final String KEY = "mdoc.threads";

  public ThreadsProvider() {
    this(new Properties());
  }

  public ThreadsProvider(Properties properties) {
    this.properties = checkNotNull(properties);
  }

  private final Properties properties;

  @Override
  public Integer get() {
    String value = getValue(properties).toUpperCase(ENGLISH);
    int threads = -1;
    if (value.endsWith("C")) {
      String trimmed = removeEnd(value, "C");
      double multiplier = parseDouble(trimmed);
      int processors = getRuntime().availableProcessors();
      threads = roundToInt(multiplier * processors, HALF_UP);
    } else {
      threads = parseInt(value);
    }
    info(LOGGER, "%s threads (%s cores)", threads, getRuntime().availableProcessors());
    return threads;
  }

  private String getValue(Properties props) {
    String defaultValue = props.getProperty(KEY, "1C");
    return System.getProperty(KEY, defaultValue);
  }

}
