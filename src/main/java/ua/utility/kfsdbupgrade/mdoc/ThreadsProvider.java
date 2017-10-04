package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.math.DoubleMath.roundToInt;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.StringUtils.removeEnd;

import java.util.Properties;

import javax.inject.Provider;

public final class ThreadsProvider implements Provider<Integer> {

  private static final String KEY = "mdoc.threads";
  // default is to use one thread per available processor
  private static final String DEFAULT_VALUE = "1C";

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
    if (value.endsWith("C")) {
      String trimmed = removeEnd(value, "C");
      double multiplier = parseDouble(trimmed);
      int processors = getRuntime().availableProcessors();
      return roundToInt(multiplier * processors, HALF_UP);
    } else {
      return parseInt(value);
    }
  }

  private String getValue(Properties props) {
    // default is to use one thread per available processor
    String defaultValue = props.getProperty(KEY, DEFAULT_VALUE);
    // always let system properties "win"
    return System.getProperty(KEY, defaultValue);
  }

}
