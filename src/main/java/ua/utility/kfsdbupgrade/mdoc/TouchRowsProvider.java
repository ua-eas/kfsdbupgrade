package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Stopwatch.createUnstarted;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocField.asMaintDocField;
import static ua.utility.kfsdbupgrade.mdoc.simple.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.simple.Lists.transform;
import static ua.utility.kfsdbupgrade.mdoc.simple.Logging.info;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.simple.Callables;
import ua.utility.kfsdbupgrade.mdoc.simple.Logging;

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
    boolean blocks = parseBoolean(props.getProperty("mdoc.blocks"));
    ExecutorService executor = new ExecutorProvider("mdoc", threads).get();
    Optional<Integer> max = absent();
    if (props.containsKey("mdoc.max")) {
      max = of(parseInt(props.getProperty("mdoc.max")));
    }
    List<RowId> rowIds = getRowIds(blocks, max);
    Stopwatch timer = createUnstarted();
    List<TouchRowsCallable> callables = new ArrayList<>();
    MaintDocField field = asMaintDocField(props.getProperty("mdoc.field", "version"));
    info(LOGGER, "selecting %s from %s rows", field, getCount(rowIds.size()));
    DataMetrics metrics = new DataMetrics();
    for (List<RowId> distribution : distribute(rowIds, threads)) {
      Connection conn = new ConnectionProvider(props, false).get();
      // callables.add(new TouchRowsCallable(conn, batchSize, distribution, metrics, timer, field));
    }
    Callables.getFutures(executor, callables);
    Logging.info(LOGGER, "total elapsed -> %s", getTime(sw));
    return sw.elapsed(MILLISECONDS);
  }

  private ImmutableList<RowId> getRowIds(boolean blocks, Optional<Integer> max) {
    RowIdConverter converter = RowIdConverter.getInstance();
    if (blocks) {
      // Map<BlockId, RowId> locations = null;
      return null; // copyOf(locations.values());
    } else {
      List<String> strings = null; // new StringProvider(new ConnectionProvider(props, false), max, "rowid").get();
      return transform(strings, converter);
    }
  }

}
