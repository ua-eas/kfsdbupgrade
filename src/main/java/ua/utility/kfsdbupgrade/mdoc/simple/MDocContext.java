package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;

public final class MDocContext {

  private final int ec2Threads;
  private final int rdsThreads;
  private final int max;
  private final int chunkSize;
  private final int selectSize;
  private final int batchSize;
  private final Function<MaintDoc, MaintDoc> converter;

  private MDocContext(Builder builder) {
    this.ec2Threads = builder.ec2Threads;
    this.rdsThreads = builder.rdsThreads;
    this.max = builder.max;
    this.chunkSize = builder.chunkSize;
    this.selectSize = builder.selectSize;
    this.batchSize = builder.batchSize;
    this.converter = builder.converter;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private int ec2Threads = -1;
    private int rdsThreads = -1;
    private int max = -1;
    private int chunkSize = -1;
    private int selectSize = -1;
    private int batchSize = -1;
    private Function<MaintDoc, MaintDoc> converter = identity();

    public Builder withEc2Threads(int ec2Threads) {
      this.ec2Threads = ec2Threads;
      return this;
    }

    public Builder withRdsThreads(int rdsThreads) {
      this.rdsThreads = rdsThreads;
      return this;
    }

    public Builder withMax(int max) {
      this.max = max;
      return this;
    }

    public Builder withChunkSize(int chunkSize) {
      this.chunkSize = chunkSize;
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

    public Builder withConverter(Function<MaintDoc, MaintDoc> converter) {
      this.converter = converter;
      return this;
    }

    public MDocContext build() {
      return validate(new MDocContext(this));
    }

    private static MDocContext validate(MDocContext instance) {
      checkArgument(instance.ec2Threads > 0, "ec2Threads must be greater than zero");
      checkArgument(instance.rdsThreads > 0, "rdsThreads must be greater than zero");
      checkArgument(instance.max > 0, "max must be greater than zero");
      checkArgument(instance.chunkSize > 0, "chunkSize must be greater than zero");
      checkArgument(instance.selectSize > 0, "selectSize must be greater than zero");
      checkArgument(instance.batchSize > 0, "batchSize must be greater than zero");
      checkNotNull(instance.converter, "converter may not be null");
      return instance;
    }
  }

  public int getEc2Threads() {
    return ec2Threads;
  }

  public int getRdsThreads() {
    return rdsThreads;
  }

  public int getMax() {
    return max;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public int getSelectSize() {
    return selectSize;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public Function<MaintDoc, MaintDoc> getConverter() {
    return converter;
  }

}
