package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.md.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.md.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.md.base.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Logging.info;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Converter;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;

public final class RowIdProvider implements Provider<ImmutableList<String>> {

  private static final Logger LOGGER = Logger.getLogger(RowIdProvider.class);

  private final Connection conn;
  private final Optional<String> schema;
  private final String table;
  private final Optional<Integer> max;
  private final Optional<Integer> show;
  private final Converter<String, RowId> converter;

  @Override
  public ImmutableList<String> get() {
    List<String> rowIds = newArrayList();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      Stopwatch sw = createStarted();
      String from = schema.isPresent() ? schema.get() + "." + table : table;
      String acquire = max.isPresent() ? "maximum of " + getCount(max.get()) : "all";
      info(LOGGER, "acquiring %s row ids from %s", acquire, from);
      stmt = conn.createStatement();
      rs = stmt.executeQuery(format("SELECT ROWID FROM %s", from));
      while (rs.next()) {
        String rowId = rs.getString(1);
        rowIds.add(rowId);
        if (show.isPresent() && rowIds.size() % show.get() == 0) {
          info(LOGGER, "%s", getCount(rowIds.size()));
        }
        if (max.isPresent() && rowIds.size() == max.get()) {
          break;
        }
      }
      String tp = getThroughputInSeconds(sw.elapsed(MILLISECONDS), rowIds.size(), "row ids/sec");
      info(LOGGER, "acquired %s row ids from %s in %s [%s]", getCount(rowIds.size()), from, getTime(sw), tp);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
    return copyOf(rowIds);
  }

  private RowIdProvider(Builder builder) {
    this.conn = builder.conn;
    this.max = builder.max;
    this.show = builder.show;
    this.converter = builder.converter;
    this.schema = builder.schema;
    this.table = builder.table;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Connection conn;
    private Optional<String> schema = absent();
    private String table;
    private Optional<Integer> max = absent();
    private Optional<Integer> show = of(100000);
    private Converter<String, RowId> converter = RowIdConverter.getInstance();

    public Builder withConn(Connection conn) {
      this.conn = conn;
      return this;
    }

    public Builder withTable(String table) {
      this.table = table;
      return this;
    }

    public Builder withSchema(String schema) {
      return withSchema(of(schema));
    }

    public Builder withSchema(Optional<String> schema) {
      this.schema = schema;
      return this;
    }

    public Builder withMax(int max) {
      return withMax(of(max));
    }

    public Builder withMax(Optional<Integer> max) {
      this.max = max;
      return this;
    }

    public Builder withShow(int show) {
      return withShow(of(show));
    }

    public Builder withShow(Optional<Integer> show) {
      this.show = show;
      return this;
    }

    public Builder withConverter(Converter<String, RowId> converter) {
      this.converter = converter;
      return this;
    }

    public RowIdProvider build() {
      return validate(new RowIdProvider(this));
    }

    private static RowIdProvider validate(RowIdProvider instance) {
      checkNotNull(instance.conn, "conn may not be null");
      checkNotNull(instance.schema, "schema may not be null");
      checkNotNull(instance.table, "table may not be null");
      checkNotNull(instance.max, "max may not be null");
      checkNotNull(instance.show, "show may not be null");
      checkNotNull(instance.converter, "converter may not be null");
      return instance;
    }
  }

}
