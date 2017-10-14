package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.MaintDoc;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;

public final class MDocProvider implements Provider<ImmutableList<MaintDoc>> {

  public MDocProvider(Connection conn, Iterable<RowId> rowIds, int selectSize) {
    this.conn = conn;
    this.rowIds = copyOf(rowIds);
    this.selectSize = selectSize;
  }

  private final Connection conn;
  private final ImmutableList<RowId> rowIds;
  private final int selectSize;
  private final RowIdConverter converter = RowIdConverter.getInstance();

  @Override
  public ImmutableList<MaintDoc> get() {
    Statement stmt = null;
    ResultSet rs = null;
    List<MaintDoc> docs = newArrayList();
    try {
      for (List<RowId> partition : partition(rowIds, selectSize)) {
        stmt = conn.createStatement();
        rs = stmt.executeQuery(format("SELECT ROWID, DOC_CNTNT FROM KRNS_MAINT_DOC_T WHERE ROWID IN (%s)", asInClause(partition)));
        while (rs.next()) {
          String rowId = rs.getString(1);
          String content = rs.getString(2);
          // if it's blank for some reason, just ignore it
          if (isNotBlank(content)) {
            MaintDoc doc = MaintDoc.build(rowId, content);
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
    return newList(docs);
  }

  private String asInClause(Iterable<RowId> rowIds) {
    List<String> list = newArrayList();
    for (RowId rowId : rowIds) {
      list.add("'" + converter.reverse().convert(rowId) + "'");
    }
    return Joiner.on(',').join(list);
  }

}
