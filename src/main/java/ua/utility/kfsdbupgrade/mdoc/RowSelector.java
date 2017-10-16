package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ua.utility.kfsdbupgrade.md.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;
import static ua.utility.kfsdbupgrade.mdoc.MaintDocSelector.asInClause;
import static ua.utility.kfsdbupgrade.mdoc.Validation.checkNoBlanks;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
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
  private final int batchSize;
  private final ImmutableList<String> rowIds;
  private final ImmutableList<String> fields;
  private final Function<ResultSet, T> function;
  private final Optional<Integer> max;
  private final boolean discard;

  @Override
  public ImmutableList<T> get() {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      String select = Joiner.on(',').join(fields);
      String from = schema.isPresent() ? schema.get() + "." + table : table;
      if (rowIds.size() > 0) {
        return doRowIds(stmt, select, from);
      } else {
        return doSelect(stmt, select, from);
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
  }

  private ImmutableList<T> doSelect(Statement stmt, String select, String from) throws IOException {
    ResultSet rs = null;
    try {
      String where = max.isPresent() ? "WHERE ROWNUM <= " + max.get() : "";
      String sql = format("SELECT %s FROM %s %s", select, from, where).trim();
      rs = stmt.executeQuery(sql);
      return doResultSet(rs);
    } catch (Throwable e) {
      throw new IOException(e);
    } finally {
      closeQuietly(rs);
    }
  }

  private ImmutableList<T> doRowIds(Statement stmt, String select, String from) throws IOException {
    List<T> list = newArrayList();
    ResultSet rs = null;
    try {
      for (List<String> partition : partition(rowIds, batchSize)) {
        String sql = format("SELECT %s FROM %s WHERE ROWID IN (%s)", select, from, asInClause(partition));
        rs = stmt.executeQuery(sql);
        list.addAll(doResultSet(rs));
      }
    } catch (Throwable e) {
      throw new IOException(e);
    } finally {
      closeQuietly(rs);
    }
    return newList(list);
  }

  private ImmutableList<T> doResultSet(ResultSet rs) throws SQLException {
    List<T> list = newArrayList();
    while (rs.next()) {
      T instance = function.apply(rs);
      if (!discard) {
        list.add(instance);
      }
    }
    return newList(list);
  }

  private RowSelector(Builder<T> builder) {
    this.conn = builder.conn;
    this.batchSize = builder.batchSize;
    this.rowIds = copyOf(builder.rowIds);
    this.schema = builder.schema;
    this.table = builder.table;
    this.fields = copyOf(builder.fields);
    this.function = builder.function;
    this.max = builder.max;
    this.discard = builder.discard;
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  public static class Builder<T> {

    private Connection conn;
    private int batchSize = 75;
    private List<String> rowIds = newArrayList();
    private Optional<String> schema = absent();
    private String table;
    private List<String> fields = newArrayList("ROWID");
    private Optional<Integer> max = absent();
    private Function<ResultSet, T> function;
    private boolean discard;

    public Builder<T> withDiscard(boolean discard) {
      this.discard = discard;
      return this;
    }

    public Builder<T> withMax(Optional<Integer> max) {
      this.max = max;
      return this;
    }

    public Builder<T> withMax(int max) {
      return withMax(of(max));
    }

    public Builder<T> withFunction(Function<ResultSet, T> function) {
      this.function = function;
      return this;
    }

    public Builder<T> withConn(Connection conn) {
      this.conn = conn;
      return this;
    }

    public Builder<T> withBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder<T> withRowIds(List<String> rowIds) {
      this.rowIds = rowIds;
      return this;
    }

    public Builder<T> withSchema(Optional<String> schema) {
      this.schema = schema;
      return this;
    }

    public Builder<T> withField(String field) {
      return withFields(asList(field));
    }

    public Builder<T> withFields(List<String> fields) {
      this.fields = fields;
      return this;
    }

    public Builder<T> withTable(String table) {
      this.table = table;
      return this;
    }

    public RowSelector<T> build() {
      return validate(new RowSelector<T>(this));
    }

    private static <T> RowSelector<T> validate(RowSelector<T> instance) {
      checkNoBlanks(instance);
      if (instance.rowIds.size() > 0) {
        checkArgument(!instance.max.isPresent(), "max is ignored when row ids are supplied");
      }
      return instance;
    }

  }

  public int getBatchSize() {
    return batchSize;
  }

  public ImmutableList<String> getRowIds() {
    return rowIds;
  }

  public Optional<String> getSchema() {
    return schema;
  }

  public String getTable() {
    return table;
  }

  public ImmutableList<String> getFields() {
    return fields;
  }

  public Function<ResultSet, T> getFunction() {
    return function;
  }

  public Optional<Integer> getMax() {
    return max;
  }

}
