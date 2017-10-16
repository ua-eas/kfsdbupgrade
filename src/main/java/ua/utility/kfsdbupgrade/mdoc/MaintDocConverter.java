package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class MaintDocConverter implements Provider<MaintDocResult> {

  private static final Logger LOGGER = Logger.getLogger(MaintDocConverter.class);

  private final Connection connection;
  private final int batchSize;
  private final EncryptionService encryptor;
  private final MaintainableXmlConversionService converter;
  private final ExecutorService executor;
  private final boolean rowId;

  @Override
  public MaintDocResult get() {
    Stopwatch overall = createStarted();
    int converted = 0;
    int errors = 0;
    PreparedStatement pstmt = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      String field = (rowId) ? "ROWID" : "DOC_HDR_ID";
      pstmt = connection.prepareStatement(format("UPDATE KRNS_MAINT_DOC_T SET DOC_CNTNT = ? WHERE %s = ?", field));
      stmt = connection.createStatement();
      rs = stmt.executeQuery(format("SELECT %s, DOC_CNTNT FROM KRNS_MAINT_DOC_T", field));
      List<MaintDoc> docs = newArrayList();
      Stopwatch sw = createStarted();
      while (rs.next()) {
        String docHeaderId = rs.getString(1);
        String content = rs.getString(2);
        docs.add(MaintDoc.build(docHeaderId, content));
        if (docs.size() % batchSize == 0) {
          MaintDocResult mdr = handleDocs(pstmt, docs);
          errors += mdr.getErrors();
          converted += mdr.getConverted();
          progress(sw, docs.size(), overall, converted);
          sw = createStarted();
          docs = newArrayList();
        }
      }
      if (docs.size() > 0) {
        MaintDocResult mdr = handleDocs(pstmt, docs);
        errors += mdr.getErrors();
        converted += mdr.getConverted();
        progress(sw, docs.size(), overall, converted);
      }
      connection.commit();
    } catch (Throwable e) {
      rollback(connection);
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(pstmt);
    }
    return new MaintDocResult(converted, errors, overall.elapsed(MILLISECONDS));
  }

  private void progress(Stopwatch sw, int count, Stopwatch overall, int converted) {
    long elapsed = sw.elapsed(MILLISECONDS);
    long overallElapsed = overall.elapsed(MILLISECONDS);
    String tp1 = getThroughputInSeconds(elapsed, count, "docs/second");
    String tp2 = getThroughputInSeconds(overallElapsed, converted, "docs/second");
    Object[] args = { getCount(count), getTime(elapsed), tp1, getCount(converted), getTime(overallElapsed), tp2 };
    LOGGER.info(format("%s docs in %s, %s [%s in %s, %s overall]", args));

  }

  private void rollback(Connection conn) {
    try {
      conn.rollback();
    } catch (Exception e) {
      LOGGER.error("unexpected error rolling back", e);
    }
  }

  private MaintDocResult handleDocs(PreparedStatement pstmt, Iterable<MaintDoc> docs) throws SQLException {
    Stopwatch sw = createStarted();
    List<ConversionResult> results = convert(docs);
    int batched = doBatch(pstmt, results);
    int errors = doErrors(results);
    return new MaintDocResult(batched, errors, sw.elapsed(MILLISECONDS));
  }

  private int doBatch(PreparedStatement pstmt, Iterable<ConversionResult> results) throws SQLException {
    int batched = 0;
    for (ConversionResult result : results) {
      if (result.getNewDocument().isPresent()) {
        MaintDoc doc = result.getNewDocument().get();
        pstmt.setString(1, doc.getContent());
        pstmt.setString(2, doc.getId());
        pstmt.addBatch();
        batched++;
      }
    }
    pstmt.executeBatch();
    return batched;
  }

  private int doErrors(Iterable<ConversionResult> results) {
    int errors = 0;
    for (ConversionResult result : results) {
      if (result.getException().isPresent()) {
        MaintDoc doc = result.getOldDocument();
        Throwable e = result.getException().get();
        LOGGER.error("error converting document " + doc.getId(), e);
        errors++;
      }
    }
    return errors;
  }

  private ImmutableList<ConversionResult> convert(Iterable<MaintDoc> docs) {
    List<Callable<ConversionResult>> callables = getCallables(docs);
    List<Future<ConversionResult>> futures = submit(executor, callables);
    return getFutures(futures);
  }

  private ImmutableList<Callable<ConversionResult>> getCallables(Iterable<MaintDoc> documents) {
    MaintDocFunction function = new MaintDocFunction(encryptor, converter);
    List<Callable<ConversionResult>> list = newArrayList();
    for (MaintDoc document : documents) {
      list.add(new MaintDocCallable(function, document));
    }
    return copyOf(list);
  }

  private static <T> ImmutableList<Future<T>> submit(ExecutorService executor, Iterable<? extends Callable<T>> callables) {
    List<Future<T>> futures = newArrayList();
    for (Callable<T> callable : callables) {
      Future<T> future = executor.submit(callable);
      futures.add(future);
    }
    return copyOf(futures);
  }

  private static <T> ImmutableList<T> getFutures(Iterable<Future<T>> futures) {
    List<T> elements = newArrayList();
    for (Future<T> future : futures) {
      elements.add(getUnchecked(future));
    }
    return copyOf(elements);
  }

  private static <T> T getUnchecked(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  private MaintDocConverter(Builder builder) {
    this.connection = builder.connection;
    this.batchSize = builder.batchSize;
    this.encryptor = builder.encryptor;
    this.converter = builder.converter;
    this.executor = builder.executor;
    this.rowId = builder.rowId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Connection connection;
    private int batchSize;
    private EncryptionService encryptor;
    private MaintainableXmlConversionService converter;
    private ExecutorService executor;
    private boolean rowId = true;

    public Builder withRowId(boolean rowId) {
      this.rowId = rowId;
      return this;
    }

    public Builder withExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    public Builder withConnection(Connection connection) {
      this.connection = connection;
      return this;
    }

    public Builder withBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder withEncryptor(EncryptionService encryptor) {
      this.encryptor = encryptor;
      return this;
    }

    public Builder withConverter(MaintainableXmlConversionService converter) {
      this.converter = converter;
      return this;
    }

    public MaintDocConverter build() {
      return validate(new MaintDocConverter(this));
    }

    private static MaintDocConverter validate(MaintDocConverter instance) {
      checkNotNull(instance.connection, "connection may not be null");
      checkArgument(instance.batchSize > 0, "batchSize must be greater than zero");
      checkNotNull(instance.encryptor, "encryptor may not be null");
      checkNotNull(instance.converter, "converter may not be null");
      return instance;
    }
  }

}
