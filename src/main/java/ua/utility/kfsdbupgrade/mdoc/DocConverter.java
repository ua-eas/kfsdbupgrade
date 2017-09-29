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
  private final MDocMetrics metrics;
  private final int batchSize;
  private final int selectSize;
  private final Function<MaintDoc, MaintDoc> function;

  public Long get() {
    Stopwatch sw = createStarted();
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = provider.get();
      pstmt = conn.prepareStatement("UPDATE KRNS_MAINT_DOC_T SET DOC_CNTNT = ? WHERE ROWID = ?");
      sw = metrics.getUpdate().elapsed(sw);
      for (List<String> partition : partition(headerIds, selectSize)) {
        List<MaintDoc> original = new MaintDocSelector(conn, partition, metrics).get();
        sw = createStarted();
        List<MaintDoc> converted = transform(original, function);
        sw = metrics.getConvert().increment(converted.size(), sum(original) + sum(converted), sw);
        int batched = 0;
        for (MaintDoc doc : converted) {
          pstmt.setString(1, doc.getContent());
          pstmt.setString(2, doc.getDocHeaderId());
          pstmt.addBatch();
          synchronized (metrics) {
            sw = metrics.getUpdate().increment(doc.getContent().length(), sw);
            if (metrics.getUpdate().getCount().getValue() % 1000 == 0) {
              new ProgressProvider(metrics).get();
            }
          }
          batched++;
          if (batched % batchSize == 0) {
            sw = createStarted();
            pstmt.executeBatch();
            metrics.getUpdate().elapsed(sw);
          }
        }
        if (batched % batchSize != 0) {
          sw = createStarted();
          pstmt.executeBatch();
          metrics.getUpdate().elapsed(sw);
        }
      }
      sw = createStarted();
      conn.commit();
      metrics.getUpdate().elapsed(sw);
      new ProgressProvider(metrics, "commit").get();
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
      sum += doc.getContent().length();
    }
    return sum;
  }

  private DocConverter(Builder builder) {
    this.provider = builder.provider;
    this.headerIds = copyOf(builder.headerIds);
    this.metrics = builder.metrics;
    this.batchSize = builder.batchSize;
    this.selectSize = builder.selectSize;
    this.function = builder.function;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Provider<Connection> provider;
    private List<String> headerIds;
    private MDocMetrics metrics;
    private int batchSize = -1;
    private int selectSize = -1;
    private Function<MaintDoc, MaintDoc> function;

    public Builder withProvider(Provider<Connection> provider) {
      this.provider = provider;
      return this;
    }

    public Builder withHeaderIds(List<String> headerIds) {
      this.headerIds = headerIds;
      return this;
    }

    public Builder withMetrics(MDocMetrics metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder withSelectSize(int selectSize) {
      this.selectSize = selectSize;
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
      checkNotNull(instance.metrics, "metrics may not be null");
      checkArgument(instance.batchSize > 0, "batchSize must be greater than zero");
      checkArgument(instance.selectSize > 0, "selectSize must be greater than zero");
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

  public int getBatchSize() {
    return batchSize;
  }

  public Function<MaintDoc, MaintDoc> getFunction() {
    return function;
  }

  public MDocMetrics getMetrics() {
    return metrics;
  }

  public int getSelectSize() {
    return selectSize;
  }

}
