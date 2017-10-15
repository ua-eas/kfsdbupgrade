package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.math.DoubleMath.roundToInt;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.StringUtils.removeEnd;

import java.util.Properties;

import javax.inject.Provider;

public final class ThreadsProvider implements Provider<Integer> {

  public ThreadsProvider(String key, Properties props, int cores) {
    checkArgument(cores > 0, "cores must be greater than zero");
    this.props = checkNotNull(props);
    this.key = checkNotNull(key);
    this.cores = cores;
  }

  private final Properties props;
  private final String key;
  private final int cores;

  @Override
  public Integer get() {
    String value = props.getProperty(key, "1C").toUpperCase(ENGLISH);
    int threads = -1;
    if (value.endsWith("C")) {
      String trimmed = removeEnd(value, "C");
      double multiplier = parseDouble(trimmed);
      threads = roundToInt(multiplier * cores, HALF_UP);
    } else {
      threads = parseInt(value);
    }
    return threads;
  }

}
