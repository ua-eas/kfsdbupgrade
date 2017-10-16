package ua.utility.kfsdbupgrade.md;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ua.utility.kfsdbupgrade.md.Closeables.closeQuietly;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import com.google.common.collect.ImmutableList;

public final class MDocProvider implements Provider<ImmutableList<MaintDoc>> {

  public MDocProvider(Connection conn, Iterable<String> rowIds, int selectSize) {
    this.conn = conn;
    this.rowIds = copyOf(rowIds);
    this.selectSize = selectSize;
  }

  private final Connection conn;
  private final ImmutableList<String> rowIds;
  private final int selectSize;

  @Override
  public ImmutableList<MaintDoc> get() {
    Statement stmt = null;
    ResultSet rs = null;
    List<MaintDoc> docs = newArrayList();
    try {
      stmt = conn.createStatement();
      for (List<String> partition : partition(rowIds, selectSize)) {
        rs = stmt.executeQuery(format("SELECT ROWID, DOC_HDR_ID, DOC_CNTNT FROM KRNS_MAINT_DOC_T WHERE ROWID IN (%s)", asInClause(partition)));
        while (rs.next()) {
          String rowId = rs.getString(1);
          String docId = rs.getString(2);
          String content = rs.getString(3);
          // if the content is blank for some reason, just ignore it
          if (isNotBlank(content)) {
            MaintDoc doc = MaintDoc.build(rowId, docId, content);
            docs.add(doc);
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

  private String asInClause(Iterable<String> rowIds) {
    StringBuilder sb = new StringBuilder();
    for (String rowId : rowIds) {
      sb.append("'");
      sb.append(rowId);
      sb.append("',");
    }
    return removeEnd(sb.toString(), ",");
  }

}
