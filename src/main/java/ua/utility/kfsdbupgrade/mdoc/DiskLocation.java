package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.hash;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ComparisonChain;

public final class DiskLocation implements Comparable<DiskLocation> {

  public DiskLocation(long fileNumber, long blockNumber) {
    this.fileNumber = fileNumber;
    this.blockNumber = blockNumber;
    this.hash = hash(fileNumber, blockNumber);
  }

  private final long fileNumber;
  private final long blockNumber;
  private final int hash;

  @Override
  public int compareTo(DiskLocation other) {
    ComparisonChain chain = ComparisonChain.start();
    chain = chain.compare(fileNumber, other.fileNumber);
    chain = chain.compare(blockNumber, other.blockNumber);
    return chain.result();
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    } else if (object == null || object.getClass() != getClass()) {
      return false;
    } else {
      DiskLocation other = (DiskLocation) object;
      return compareTo(other) == 0;
    }
  }

  @Override
  public String toString() {
    ToStringHelper helper = toStringHelper(this);
    helper.add("fileNumber", fileNumber);
    helper.add("blockNumber", blockNumber);
    return helper.toString();
  }

}
