package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableMap.copyOf;
import static java.lang.Math.round;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;

public final class BlockProvider implements Provider<ImmutableMap<BlockId, RowId>> {

  private static final Logger LOGGER = getLogger(BlockProvider.class);

  public BlockProvider(Connection conn, String schema, String table, Optional<Integer> max, Optional<Integer> show) {
    this.conn = conn;
    this.schema = schema.toUpperCase();
    this.table = table.toUpperCase();
    this.max = max;
    this.show = show;
  }

  private final Connection conn;
  private final String schema;
  private final String table;
  private final Optional<Integer> max;
  private final Optional<Integer> show;
  private final RowIdConverter converter = new RowIdConverter();

  public ImmutableMap<BlockId, RowId> get() {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      info(LOGGER, "acquiring -> %s rowids in %s.%s", max.isPresent() ? getCount(max.get()) : "all", schema, table);
      Stopwatch sw = createStarted();
      stmt = conn.createStatement();
      String where = max.isPresent() ? "WHERE ROWNUM <= " + max.get() : "";
      rs = stmt.executeQuery(format("SELECT ROWID FROM %s.%s %s", schema, table, where).trim());
      int count = 0;
      ListMultimap<BlockId, RowId> mm = ArrayListMultimap.create();
      while (rs.next()) {
        String string = rs.getString(1);
        RowId rowId = converter.convert(string);
        mm.put(rowId.getBlock(), rowId);
        count++;
        if (show.isPresent() && count % show.get() == 0) {
          info(LOGGER, "%s of %s [%s]", getCount(count), max.isPresent() ? getCount(max.get()) : "?", getTime(sw));
        }
      }
      int rows = mm.size();
      int blocks = mm.keySet().size();
      double percent = (blocks * 1d) / rows;
      double reduction = (1 - percent) * 100;
      info(LOGGER, "elapsed ----> %s", getTime(sw));
      info(LOGGER, "rows -------> %s", getCount(rows));
      info(LOGGER, "blocks -----> %s", getCount(blocks));
      info(LOGGER, "reduction --> %s%%", round(reduction));
      info(LOGGER, "blocks/row -> %s%%", getCount(blocks / rows));
      Map<BlockId, RowId> map = new LinkedHashMap<>();
      for (BlockId block : mm.keySet()) {
        map.put(block, mm.get(block).iterator().next());
      }
      return copyOf(map);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }

  }

}
