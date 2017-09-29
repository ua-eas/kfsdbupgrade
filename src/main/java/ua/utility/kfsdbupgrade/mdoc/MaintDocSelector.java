package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public class MaintDocSelector implements Provider<ImmutableList<MaintDoc>> {

  public MaintDocSelector(Connection conn, Iterable<String> headerIds, MDocMetrics metrics) {
    this.conn = checkNotNull(conn);
    this.headerIds = copyOf(headerIds);
    this.metrics = checkNotNull(metrics);
  }

  private final Connection conn;
  private final ImmutableList<String> headerIds;
  private final MDocMetrics metrics;

  public ImmutableList<MaintDoc> get() {
    List<MaintDoc> docs = newArrayList();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      Stopwatch sw = createStarted();
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT ROWID, DOC_CNTNT FROM KRNS_MAINT_DOC_T WHERE ROWID IN (" + asInClause(headerIds) + ")");
      while (rs.next()) {
        String headerId = rs.getString(1);
        String content = rs.getString(2);
        docs.add(MaintDoc.build(headerId, content));
        sw = metrics.getSelect().increment(headerId.length() + content.length(), sw);
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
    return copyOf(docs);
  }

  private String asInClause(Iterable<String> strings) {
    List<String> list = newArrayList();
    for (String string : strings) {
      list.add("'" + string + "'");
    }
    return Joiner.on(',').join(list);
  }

}
