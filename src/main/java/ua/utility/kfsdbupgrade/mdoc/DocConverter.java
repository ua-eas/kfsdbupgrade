package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.partition;
import static com.google.common.collect.Lists.transform;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class DocConverter implements Provider<Long> {

  private final Provider<Connection> provider;
  private final ImmutableList<String> headerIds;
  private final DataMetrics select;
  private final DataMetrics update;
  private final DataMetrics convert;
  private final int batchSize;
  private final Function<MaintDoc, MaintDoc> function;

  public Long get() {
    Stopwatch sw = createStarted();
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = provider.get();
      pstmt = conn.prepareStatement("UPDATE KRNS_MAINT_DOC_T SET DOC_CNTNT = ? WHERE DOC_HDR_ID = ?");
      sw = update.elapsed(sw);
      for (List<String> partition : partition(headerIds, batchSize)) {
        List<MaintDoc> original = new MaintDocSelector(conn, partition, select).get();
        sw = createStarted();
        List<MaintDoc> converted = transform(original, function);
        sw = convert.increment(converted.size(), sum(original) + sum(converted), sw);
        for (MaintDoc doc : converted) {
          pstmt.setString(1, doc.getContent());
          pstmt.setString(2, doc.getDocHeaderId());
          pstmt.addBatch();
          sw = update.increment(doc.getDocHeaderId().length() + doc.getContent().length(), sw);
        }
        sw = createStarted();
        pstmt.executeBatch();
        update.elapsed(sw);
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(pstmt);
      closeQuietly(conn);
    }
    return sw.elapsed(MILLISECONDS);
  }

  private long sum(Iterable<MaintDoc> docs) {
    long sum = 0;
    for (MaintDoc doc : docs) {
      sum += doc.getDocHeaderId().length() + doc.getContent().length();
    }
    return sum;
  }

  private DocConverter(Builder builder) {
    this.provider = builder.provider;
    this.headerIds = copyOf(builder.headerIds);
    this.select = builder.select;
    this.update = builder.update;
    this.convert = builder.convert;
    this.batchSize = builder.batchSize;
    this.function = builder.function;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Provider<Connection> provider;
    private List<String> headerIds;
    private DataMetrics select;
    private DataMetrics update;
    private DataMetrics convert;
    private int batchSize;
    private Function<MaintDoc, MaintDoc> function;

    public Builder withProvider(Provider<Connection> provider) {
      this.provider = provider;
      return this;
    }

    public Builder withHeaderIds(List<String> headerIds) {
      this.headerIds = headerIds;
      return this;
    }

    public Builder withSelect(DataMetrics select) {
      this.select = select;
      return this;
    }

    public Builder withUpdate(DataMetrics update) {
      this.update = update;
      return this;
    }

    public Builder withConvert(DataMetrics convert) {
      this.convert = convert;
      return this;
    }

    public Builder withBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder withFunction(Function<MaintDoc, MaintDoc> function) {
      this.function = function;
      return this;
    }

    public DocConverter build() {
      return validate(new DocConverter(this));
    }

    private static DocConverter validate(DocConverter instance) {
      checkNotNull(instance.provider, "provider may not be null");
      checkNotNull(instance.headerIds, "headerIds may not be null");
      checkNotNull(instance.select, "select may not be null");
      checkNotNull(instance.update, "update may not be null");
      checkNotNull(instance.convert, "convert may not be null");
      checkArgument(instance.batchSize > 0, "batchSize must be greater than zero");
      checkNotNull(instance.function, "function may not be null");
      return instance;
    }
  }

  public Provider<Connection> getProvider() {
    return provider;
  }

  public ImmutableList<String> getHeaderIds() {
    return headerIds;
  }

  public DataMetrics getSelect() {
    return select;
  }

  public DataMetrics getUpdate() {
    return update;
  }

  public DataMetrics getConvert() {
    return convert;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public Function<MaintDoc, MaintDoc> getFunction() {
    return function;
  }

}
