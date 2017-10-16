package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkNotNull;
import static ua.utility.kfsdbupgrade.md.base.Logging.info;

import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

public final class Ec2ThreadsProvider implements Provider<Integer> {

  private static final Logger LOGGER = Logger.getLogger(Ec2ThreadsProvider.class);

  public Ec2ThreadsProvider(Properties props) {
    this.props = checkNotNull(props);
  }

  private final Properties props;

  public Integer get() {
    int cores = Runtime.getRuntime().availableProcessors();
    int threads = new ThreadsProvider(props, "ec2.threads", cores).get();
    info(LOGGER, "ec2 cores ---> %s", cores);
    info(LOGGER, "ec2 threads -> %s", threads);
    return threads;
  }

}
