package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.hash;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ComparisonChain;

public final class BlockId implements Comparable<BlockId> {

  public BlockId(long fileNumber, long blockNumber) {
    checkArgument(fileNumber >= 0, "fileNumber cannot be negative");
    checkArgument(blockNumber >= 0, "blockNumber cannot be negative");
    this.fileNumber = fileNumber;
    this.blockNumber = blockNumber;
    this.hash = hash(fileNumber, blockNumber);
  }

  private final long fileNumber;
  private final long blockNumber;
  private final int hash;

  @Override
  public int compareTo(BlockId other) {
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
      BlockId other = (BlockId) object;
      return (hash == other.hash) && compareTo(other) == 0;
    }
  }

  @Override
  public String toString() {
    ToStringHelper helper = toStringHelper(this);
    helper.add("fileNumber", fileNumber);
    helper.add("blockNumber", blockNumber);
    return helper.toString();
  }

  public long getFileNumber() {
    return fileNumber;
  }

  public long getBlockNumber() {
    return blockNumber;
  }

}
