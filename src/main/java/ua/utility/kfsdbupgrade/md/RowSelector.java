package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ua.utility.kfsdbupgrade.md.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.md.Jdbc.asInClause;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public final class RowSelector<T> implements Provider<ImmutableList<T>> {

  private final Connection conn;
  private final Optional<String> schema;
  private final String table;
  private final ImmutableList<String> fields;
  private final ImmutableList<String> rowIds;
  private final Function<ResultSet, T> function;

  @Override
  public ImmutableList<T> get() {
    List<T> list = newArrayList();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      String select = Joiner.on(',').join(fields);
      String from = schema.isPresent() ? schema.get() + "." + table : table;
      stmt = conn.createStatement();
      rs = stmt.executeQuery(format("SELECT %s FROM %s WHERE ROWID IN (%s)", select, from, asInClause(rowIds, true)));
      while (rs.next()) {
        T instance = function.apply(rs);
        list.add(instance);
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
    return copyOf(list);
  }

  private RowSelector(Builder<T> builder) {
    this.conn = builder.conn;
    this.schema = builder.schema;
    this.table = builder.table;
    this.rowIds = copyOf(builder.rowIds);
    this.fields = copyOf(builder.fields);
    this.function = builder.function;
  }

  public static <T> RowSelector<T> build(Connection conn, String table, Iterable<String> rowIds) {
    Builder<T> builder = builder();
    builder.withConn(conn);
    builder.withTable(table);
    builder.withRowIds(copyOf(rowIds));
    return builder.build();
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  public static class Builder<T> {

    private Connection conn;
    private Optional<String> schema = absent();
    private String table;
    private List<String> rowIds;
    private Function<ResultSet, T> function;
    private List<String> fields;

    public Builder<T> withField(String field) {
      return withFields(asList(field));
    }

    public Builder<T> withFields(List<String> fields) {
      this.fields = fields;
      return this;
    }

    public Builder<T> withRowIds(List<String> rowIds) {
      this.rowIds = rowIds;
      return this;
    }

    public Builder<T> withConn(Connection conn) {
      this.conn = conn;
      return this;
    }

    public Builder<T> withTable(String table) {
      this.table = table;
      return this;
    }

    public Builder<T> withSchema(String schema) {
      return withSchema(of(schema));
    }

    public Builder<T> withSchema(Optional<String> schema) {
      this.schema = schema;
      return this;
    }

    public RowSelector<T> build() {
      return validate(new RowSelector<T>(this));
    }

    private static <T> RowSelector<T> validate(RowSelector<T> instance) {
      checkNotNull(instance.conn, "conn may not be null");
      checkNotNull(instance.schema, "schema may not be null");
      checkNotNull(instance.table, "table may not be null");
      checkArgument(instance.rowIds.size() > 0, "rowIds can't be empty");
      checkArgument(instance.fields.size() > 0, "fields can't be empty");
      return instance;
    }
  }

}
