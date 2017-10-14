package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.inject.Provider;

import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.MaintDoc;

public final class MDocUpdater implements Provider<Long> {

  public MDocUpdater(Connection conn, Iterable<MaintDoc> docs, int batchSize) {
    this.conn = conn;
    this.docs = copyOf(docs);
    this.batchSize = batchSize;
  }

  private final Connection conn;
  private final ImmutableList<MaintDoc> docs;
  private final int batchSize;

  @Override
  public Long get() {
    PreparedStatement pstmt = null;
    try {
      pstmt = conn.prepareStatement("UPDATE KRNS_MAINT_DOC_T SET DOC_CNTNT = ? WHERE ROWID = ?");
      for (List<MaintDoc> partition : partition(docs, batchSize)) {
        for (MaintDoc doc : partition) {
          pstmt.setString(1, doc.getContent());
          pstmt.setString(2, doc.getId());
          pstmt.addBatch();
        }
        pstmt.executeBatch();
      }
      conn.commit();
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(pstmt);
    }
    return 0L;
  }

}
