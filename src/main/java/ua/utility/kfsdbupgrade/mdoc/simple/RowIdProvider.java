package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Converter;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;

public final class RowIdProvider implements Provider<ImmutableList<RowId>> {

  private static final Logger LOGGER = Logger.getLogger(RowIdProvider.class);

  private final Connection conn;
  private final Optional<String> schema;
  private final String table;
  private final Optional<Integer> max;
  private final Optional<Integer> show;
  private final Converter<String, RowId> converter;

  @Override
  public ImmutableList<RowId> get() {
    List<RowId> rowIds = newArrayList();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      String from = schema.isPresent() ? schema.get() + "." + table : table;
      rs = stmt.executeQuery(format("SELECT ROWID FROM %s", from));
      while (rs.next()) {
        String string = rs.getString(1);
        RowId rowId = converter.convert(string);
        rowIds.add(rowId);
        if (show.isPresent() && rowIds.size() % show.get() == 0) {
          info(LOGGER, "%s", getCount(rowIds.size()));
        }
        if (max.isPresent() && rowIds.size() == max.get()) {
          break;
        }
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
    return newList(rowIds);
  }

  private RowIdProvider(Builder builder) {
    this.conn = builder.conn;
    this.max = builder.max;
    this.show = builder.show;
    this.converter = builder.converter;
    this.schema = builder.schema;
    this.table = builder.table;
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
