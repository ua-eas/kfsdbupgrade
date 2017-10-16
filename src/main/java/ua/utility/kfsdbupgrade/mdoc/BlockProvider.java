package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableMap.copyOf;
import static java.lang.Math.round;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.base.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.base.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;

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

  private final Provider<Connection> provider;
  private final Optional<String> schema;
  private final String table;
  private final Optional<Integer> max;
  private final Optional<Integer> show;
  private final RowIdConverter converter = RowIdConverter.getInstance();

  public ImmutableMap<BlockId, RowId> get() {
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      String from = schema.isPresent() ? schema.get() + "." + table : table;
      info(LOGGER, "acquiring -> %s rowids for %s", max.isPresent() ? getCount(max.get()) : "all", from);
      Stopwatch sw = createStarted();
      stmt = provider.get().createStatement();
      String where = max.isPresent() ? "WHERE ROWNUM <= " + max.get() : "";
      rs = stmt.executeQuery(format("SELECT ROWID FROM %s %s", from.toUpperCase(), where).trim());
      int count = 0;
      ListMultimap<BlockId, RowId> mm = ArrayListMultimap.create();
      while (rs.next()) {
        String string = rs.getString(1);
        RowId rowId = converter.convert(string);
        mm.put(rowId.getBlock(), rowId);
        count++;
        if (show.isPresent() && (count % show.get()) == 0) {
          String throughput = getThroughputInSeconds(sw, count, "rows/second");
          info(LOGGER, "%s of %s [%s] %s", getCount(count), max.isPresent() ? getCount(max.get()) : "?", getTime(sw), throughput);
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
      info(LOGGER, "rows/block -> %s", getCount(rows / blocks));
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
      closeQuietly(conn);
    }

  }

  private BlockProvider(Builder builder) {
    this.provider = builder.provider;
    this.schema = builder.schema;
    this.table = builder.table;
    this.max = builder.max;
    this.show = builder.show;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Provider<Connection> provider;
    private Optional<String> schema = absent();
    private String table;
    private Optional<Integer> max = absent();
    private Optional<Integer> show = of(1000);

    public Builder withProvider(Provider<Connection> provider) {
      this.provider = provider;
      return this;
    }

    public Builder withSchema(Optional<String> schema) {
      this.schema = schema;
      return this;
    }

    public Builder withTable(String table) {
      this.table = table;
      return this;
    }

    public Builder withMax(Optional<Integer> max) {
      this.max = max;
      return this;
    }

    public Builder withMax(int max) {
      return withMax(of(max));
    }

    public Builder withShow(Optional<Integer> show) {
      this.show = show;
      return this;
    }

    public Builder withShow(int show) {
      return withShow(of(show));
    }

    public BlockProvider build() {
      return validate(new BlockProvider(this));
    }

    private static BlockProvider validate(BlockProvider instance) {
      checkNotNull(instance.provider, "provider may not be null");
      checkNotNull(instance.schema, "schema may not be null");
      checkNotNull(instance.table, "table may not be null");
      checkNotNull(instance.max, "max may not be null");
      checkNotNull(instance.show, "show may not be null");
      checkNotNull(instance.converter, "converter may not be null");
      return instance;
    }
  }

}
