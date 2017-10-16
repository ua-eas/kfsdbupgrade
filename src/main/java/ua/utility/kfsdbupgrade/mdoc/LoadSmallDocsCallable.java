package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.md.Formats.getCount;
import static ua.utility.kfsdbupgrade.md.Formats.getRate;
import static ua.utility.kfsdbupgrade.md.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.md.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Stopwatches.synchronizedStart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class LoadSmallDocsCallable implements Callable<Long> {

  private static final Logger LOGGER = getLogger(LoadSmallDocsCallable.class);

  public LoadSmallDocsCallable(Connection conn, int batchSize, Iterable<MaintDoc> docs, Counter counter, Counter bytes, Stopwatch sw, int iterations) {
    this.conn = checkNotNull(conn);
    this.batchSize = batchSize;
    this.docs = copyOf(docs);
    this.counter = counter;
    this.bytes = bytes;
    this.sw = sw;
    this.iterations = iterations;
  }

  private final Connection conn;
  private final int batchSize;
  private final ImmutableList<MaintDoc> docs;
  private final Counter counter;
  private final Counter bytes;
  private final Stopwatch sw;
  private final int iterations;

  @Override
  public Long call() {
    synchronizedStart(sw);
    Stopwatch overall = createStarted();
    PreparedStatement pstmt = null;
    try {
      String insert = "INSERT INTO KRNS_MAINT_DOC_T (DOC_HDR_ID, DOC_CNTNT) VALUES (?,?)";
      pstmt = conn.prepareStatement(insert);
      for (int i = 0; i < iterations; i++) {
        for (List<MaintDoc> partition : partition(docs, batchSize)) {
          for (MaintDoc document : partition) {
            int sequence;
            String documentHeaderId;
            synchronized (counter) {
              // the document header id from the object is not unique across threads
              sequence = checkedCast(counter.increment());
              documentHeaderId = Integer.toString(sequence);
              bytes.increment(documentHeaderId.length() + document.getContent().length());
              if (sequence % 1000 == 0) {
                long elapsed = sw.elapsed(MILLISECONDS);
                String rate = getRate(elapsed, bytes.getValue());
                info("inserted -> %s docs in %s [%s] %s", getCount(sequence), getTime(elapsed), getThroughputInSeconds(elapsed, sequence, "docs/second"), rate);
              }
            }
            pstmt.setString(1, documentHeaderId);
            pstmt.setString(2, document.getContent());
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
      }
      conn.commit();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(pstmt);
      closeQuietly(conn);
    }
    return overall.elapsed(MILLISECONDS);
  }

  private void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

  public Connection getConn() {
    return conn;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public ImmutableList<MaintDoc> getDocs() {
    return docs;
  }

}
