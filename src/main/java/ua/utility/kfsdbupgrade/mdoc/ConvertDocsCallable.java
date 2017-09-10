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
import static ua.utility.kfsdbupgrade.mdoc.Formats.getRate;
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

public final class ConvertDocsCallable implements Callable<BatchResult> {

  private static final Logger LOGGER = getLogger(ConvertDocsCallable.class);

  public ConvertDocsCallable(Provider<Connection> provider, int batchSize, Iterable<String> docHeaderIds, MaintainableXmlConversionService converter, EncryptionService encryptor) {
    this.provider = checkNotNull(provider);
    this.batchSize = batchSize;
    this.docHeaderIds = copyOf(docHeaderIds);
    this.function = new MaintDocFunction(encryptor, converter);
  }

  private final Provider<Connection> provider;
  private final int batchSize;
  private final ImmutableList<String> docHeaderIds;
  private final MaintDocFunction function;

  @Override
  public BatchResult call() {
    Connection conn = null;
    PreparedStatement pstmt = null;
    BatchResult br = new BatchResult(0, 0, 0);
    try {
      conn = provider.get();
      pstmt = conn.prepareStatement("UPDATE KRNS_MAINT_DOC_T SET DOC_CNTNT = ? WHERE DOC_HDR_ID = ?");
      for (List<String> partition : partition(docHeaderIds, batchSize)) {
        List<MaintDoc> selected = select(conn, partition);
        Iterable<ConversionResult> converted = transform(selected, function);
        br = BatchResult.add(br, batch(conn, pstmt, converted));
        if (br.getCount() % 1000 == 0) {
          progress(br, docHeaderIds.size());
        }
        if (br.getCount() > 1500) {
          break;
        }
      }
      conn.commit();
      progress(br, br.getCount());
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(pstmt);
      closeQuietly(conn);
    }
    return br;
  }

  private BatchResult batch(Connection conn, PreparedStatement pstmt, Iterable<ConversionResult> results) throws SQLException {
    int count = 0;
    long bytes = 0;
    Stopwatch sw = createStarted();
    for (ConversionResult result : results) {
      if (result.getNewDocument().isPresent()) {
        MaintDoc newDoc = result.getNewDocument().get();
        pstmt.setString(1, newDoc.getContent());
        pstmt.setString(2, newDoc.getDocHeaderId());
        pstmt.addBatch();
        count++;
        bytes += newDoc.getContent().length() + newDoc.getDocHeaderId().length();
      }
    }
    pstmt.executeBatch();
    return new BatchResult(count, bytes, sw.elapsed(MILLISECONDS));
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
        docs.add(MaintDoc.build(docHeaderId, content));
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
    return ImmutableList.copyOf(docs);
  }

  private void progress(BatchResult br, int total) {
    String rate = getRate(br.getElapsed(), br.getBytes());
    String throughput = getThroughputInSeconds(br.getElapsed(), br.getCount(), "docs/second");
    Object[] args = { getCount(br.getCount()), getCount(total), getTime(br.getElapsed()), throughput, rate };
    info("converted -> %s of %s in %s [%s, %s]", args);
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
