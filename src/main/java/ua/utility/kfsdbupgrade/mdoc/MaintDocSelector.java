package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public class MaintDocSelector implements Provider<ImmutableList<MaintDoc>> {

  private static final Logger LOGGER = getLogger(MaintDocSelector.class);

  public MaintDocSelector(Connection conn, Iterable<String> headerIds, MDocMetrics metrics, String field) {
    this.conn = checkNotNull(conn);
    this.headerIds = copyOf(headerIds);
    this.metrics = checkNotNull(metrics);
    this.field = checkNotNull(field);
  }

  private final Connection conn;
  private final ImmutableList<String> headerIds;
  private final MDocMetrics metrics;
  private final String field;

  public ImmutableList<MaintDoc> get() {
    List<MaintDoc> docs = newArrayList();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      Stopwatch sw = createStarted();
      stmt = conn.createStatement();
      rs = stmt.executeQuery(format("SELECT %s, DOC_CNTNT FROM KRNS_MAINT_DOC_T WHERE %s IN (" + asInClause(headerIds, true) + ")", field, field));
      while (rs.next()) {
        String headerId = rs.getString(1);
        String content = rs.getString(2);
        docs.add(MaintDoc.build(headerId, content));
        synchronized (metrics) {
          sw = metrics.getSelect().increment(headerId.length() + content.length(), sw);
          int selected = checkedCast(metrics.getSelect().getCount());
          if (selected % 1000 == 0) {

          }
        }
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
    return copyOf(docs);
  }

  public static String asInClause(Iterable<?> iterable, boolean quote) {
    List<String> list = newArrayList();
    for (Object element : iterable) {
      if (quote) {
        list.add("'" + element.toString() + "'");
      } else {
        list.add(element.toString());
      }
    }
    return Joiner.on(',').join(list);
  }

}
