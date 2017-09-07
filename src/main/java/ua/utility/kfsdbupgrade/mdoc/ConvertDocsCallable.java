package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class ConvertDocsCallable implements Callable<Long> {

  private static final Logger LOGGER = getLogger(ConvertDocsCallable.class);

  public ConvertDocsCallable(Provider<Connection> provider, int batchSize, Iterable<String> docHeaderIds, MaintainableXmlConversionService converter, EncryptionService encryptor) {
    this.provider = checkNotNull(provider);
    this.batchSize = batchSize;
    this.docHeaderIds = copyOf(docHeaderIds);
    this.function = new MaintDocFunction(encryptor, converter);
    this.display = 1000 / batchSize;
  }

  private final Provider<Connection> provider;
  private final int batchSize;
  private final int display;
  private final ImmutableList<String> docHeaderIds;
  private final MaintDocFunction function;

  @Override
  public Long call() {
    Stopwatch sw = createStarted();
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = provider.get();
      pstmt = conn.prepareStatement("UPDATE KRNS_MAINT_DOC_T SET DOC_CNTNT = ? WHERE DOC_HDR_ID = ?");
      int batches = 0;
      for (List<String> partition : partition(docHeaderIds, batchSize)) {
        List<MaintDoc> selected = select(conn, partition);
        Iterable<ConversionResult> converted = transform(selected, function);
        batch(conn, pstmt, sw, docHeaderIds.size(), converted);
        if (++batches % display == 0) {
          progress(sw, batches * batchSize, docHeaderIds.size());
        }
      }
      conn.commit();
      progress(sw, docHeaderIds.size(), docHeaderIds.size());
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(pstmt);
      closeQuietly(conn);
    }
    return sw.elapsed(MILLISECONDS);
  }

  private void batch(Connection conn, PreparedStatement pstmt, Stopwatch sw, int total, Iterable<ConversionResult> results) throws SQLException {
    int batched = 0;
    for (ConversionResult result : results) {
      if (result.getNewDocument().isPresent()) {
        MaintDoc newDoc = result.getNewDocument().get();
        MaintDoc oldDoc = result.getOldDocument();
        if (!newDoc.getContent().equals(oldDoc.getContent())) {
          pstmt.setString(1, newDoc.getContent());
          pstmt.setString(2, newDoc.getDocHeaderId());
          pstmt.addBatch();
          batched++;
        }
      }
    }
    if (batched > 0) {
      pstmt.executeBatch();
    }
  }

  private ImmutableList<MaintDoc> select(Connection conn, Iterable<String> docHeaderIds) {
    Statement stmt = null;
    ResultSet rs = null;
    List<MaintDoc> docs = newArrayList();
    try {
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT DOC_HDR_ID, DOC_CNTNT FROM KRNS_MAINT_DOC_T WHERE DOC_HDR_ID IN (" + Joiner.on(',').join(docHeaderIds) + ")");
      while (rs.next()) {
        String docHeaderId = rs.getString(1);
        String content = rs.getString(2);
        docs.add(new MaintDoc(docHeaderId, content));
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
    return ImmutableList.copyOf(docs);
  }

  private void progress(Stopwatch sw, int count, int total) {
    long elapsed = sw.elapsed(MILLISECONDS);
    String throughput = getThroughputInSeconds(elapsed, count, "docs/second");
    Object[] args = { getCount(count), getCount(total), getTime(elapsed), throughput };
    info("converted -> %s of %s in %s [%s]", args);
  }

  private void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

  public int getBatchSize() {
    return batchSize;
  }

}
