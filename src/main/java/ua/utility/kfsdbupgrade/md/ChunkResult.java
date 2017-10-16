package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ChunkResult {

  public ChunkResult(int count, DataMetric read, DataMetric convert, DataMetric write) {
    checkArgument(count >= 0, "count must greater than or equal to zero");
    this.count = count;
    this.read = checkNotNull(read);
    this.convert = checkNotNull(convert);
    this.write = checkNotNull(write);
  }

  private final int count;
  private final DataMetric read;
  private final DataMetric convert;
  private final DataMetric write;

  public DataMetric getRead() {
    return read;
  }

  public DataMetric getConvert() {
    return convert;
  }

  public DataMetric getWrite() {
    return write;
  }

  public int getCount() {
    return count;
  }

}
