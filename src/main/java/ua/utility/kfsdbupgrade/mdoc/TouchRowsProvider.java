package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;

import ua.utility.kfsdbupgrade.log.Logging;

public final class TouchRowsProvider implements Provider<Long> {

  private static final Logger LOGGER = getLogger(TouchRowsProvider.class);

  public TouchRowsProvider(Properties props) {
    this.props = props;
  }

  private final Properties props;

  public Long get() {
    Stopwatch sw = createStarted();
    int threads = new ThreadsProvider(props).get();
    int batchSize = parseInt(props.getProperty("mdoc.batch"));
    ExecutorService executor = new ExecutorProvider("mdoc", threads).get();
    Map<DiskLocation, RowId> locations = new DiskLocationProvider(props).get();
    List<RowId> rowIds = copyOf(locations.values());
    Counter counter = new Counter();
    Stopwatch timer = Stopwatch.createUnstarted();
    List<TouchRowsCallable> callables = new ArrayList<>();
    for (List<RowId> distribution : distribute(rowIds, threads)) {
      Connection conn = new ConnectionProvider(props, false).get();
      callables.add(new TouchRowsCallable(conn, batchSize, distribution, counter, timer));
    }
    Callables.getFutures(executor, callables);
    Logging.info(LOGGER, "elapsed -> %s", getTime(sw));
    Logging.info(LOGGER, "count ---> %s", getCount(checkedCast(counter.getValue())));
    return sw.elapsed(MILLISECONDS);
  }

}
