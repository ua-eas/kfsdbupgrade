package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.of;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.lang.Integer.parseInt;
import static java.lang.Math.round;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;

import ua.utility.kfsdbupgrade.log.Logging;

public final class DiskLocationProvider implements Provider<ImmutableMap<DiskLocation, String>> {

  private static final Logger LOGGER = getLogger(DiskLocationProvider.class);

  public DiskLocationProvider(Properties props) {
    this.props = props;
  }

  private final Properties props;

  public ImmutableMap<DiskLocation, String> get() {
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      RowIdConverter converter = new RowIdConverter();
      int count = 0;
      Optional<Integer> max = Optional.absent();
      if (props.containsKey("mdoc.rowids")) {
        int value = parseInt(props.getProperty("mdoc.rowids"));
        max = of(value);
      }
      conn = new ConnectionProvider(props, false).get();
      Stopwatch sw = createStarted();
      stmt = conn.createStatement();
      rs = stmt.executeQuery("select rowid from krns_maint_doc_t");
      ListMultimap<DiskLocation, RowId> mm = ArrayListMultimap.create();
      while (rs.next()) {
        String string = rs.getString(1);
        RowId id = converter.convert(string);
        DiskLocation location = new DiskLocation(id.getFileNumber(), id.getBlockNumber());
        mm.put(location, id);
        count++;
        if (count % 10000 == 0) {
          info(LOGGER, "%s of %s [%s]", getCount(count), getCount(max.or(-1)), getTime(sw));
          Logging.java();
        }
        if (max.isPresent() && max.get() == count) {
          break;
        }
      }
      int size = mm.size();
      int keys = mm.keySet().size();
      double percent = (keys * 1d) / size;
      double reduction = (1 - percent) * 100;
      info(LOGGER, "elapsed ---> %s", getTime(sw));
      info(LOGGER, "rows ------> %s", getCount(mm.size()));
      info(LOGGER, "locations -> %s", getCount(mm.keySet().size()));
      info(LOGGER, "reduction -> %s%%", round(reduction));
      info(LOGGER, "%s", Joiner.on('\n').join(mm.keySet()));
      Map<DiskLocation, String> map = newLinkedHashMap();
      for (DiskLocation location : mm.keySet()) {
        List<RowId> rowIds = mm.get(location);
        if (!map.containsKey(location)) {
          String rowId = rowIds.iterator().next().toString();
          map.put(location, rowId);
        }
      }
      return copyOf(map);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }

  }

}
