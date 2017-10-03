package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Stopwatch.createUnstarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocField.asMaintDocField;

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
    Stopwatch timer = createUnstarted();
    List<TouchRowsCallable> callables = new ArrayList<>();
    MaintDocField field = asMaintDocField(props.getProperty("mdoc.field", "version"));
    info(LOGGER, "selecting %s from %s rows", field, getCount(rowIds.size()));
    DataMetrics metrics = new DataMetrics();
    for (List<RowId> distribution : distribute(rowIds, threads)) {
      Connection conn = new ConnectionProvider(props, false).get();
      callables.add(new TouchRowsCallable(conn, batchSize, distribution, metrics, timer, field));
    }
    Callables.getFutures(executor, callables);
    Logging.info(LOGGER, "total elapsed -> %s", getTime(sw));
    return sw.elapsed(MILLISECONDS);
  }

}
