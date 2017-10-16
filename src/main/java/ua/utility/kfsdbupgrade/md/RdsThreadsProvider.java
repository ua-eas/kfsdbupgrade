package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.parseInt;
import static ua.utility.kfsdbupgrade.md.Logging.info;

import java.sql.Connection;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;

public final class RdsThreadsProvider implements Provider<Integer> {

  private static final Logger LOGGER = Logger.getLogger(RdsThreadsProvider.class);

  public RdsThreadsProvider(Properties props, Connection conn) {
    this.props = checkNotNull(props);
    this.conn = checkNotNull(conn);
  }

  private final Properties props;
  private final Connection conn;

  public Integer get() {
    int cores = getCores(props, conn);
    int threads = new ThreadsProvider(props, "rds.threads", cores).get();
    info(LOGGER, "rds cores ---> %s", cores);
    info(LOGGER, "rds threads -> %s", threads);
    return threads;
  }

  private int getCores(Properties props, Connection conn) {
    Optional<Integer> oracleValue = new OracleCpusProvider(conn).get();
    if (oracleValue.isPresent()) {
      return oracleValue.get();
    } else {
      return parseInt(props.getProperty("rds.cores.default", "4"));
    }
  }

}
