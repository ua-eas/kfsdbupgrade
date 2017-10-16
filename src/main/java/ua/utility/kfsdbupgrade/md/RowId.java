package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.hash;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ComparisonChain;

import ua.utility.kfsdbupgrade.mdoc.BlockId;

public final class RowId implements Comparable<RowId> {

  private final long objectId;
  private final BlockId block;
  private final long rowNumber;
  private final int hash;

  private RowId(Builder builder) {
    this.objectId = builder.objectId;
    this.block = builder.block;
    this.rowNumber = builder.rowNumber;
    this.hash = hash(objectId, block, rowNumber);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long objectId = -1;
    private BlockId block;
    private long rowNumber = -1;

    public Builder withObjectId(long objectId) {
      this.objectId = objectId;
      return this;
    }

    public Builder withBlock(BlockId block) {
      this.block = block;
      return this;
    }

    public Builder withRowNumber(long rowNumber) {
      this.rowNumber = rowNumber;
      return this;
    }

    public RowId build() {
      return validate(new RowId(this));
    }

    private static RowId validate(RowId instance) {
      checkArgument(instance.objectId >= 0L, "objectId must be greater than or equal to zero");
      checkArgument(instance.block != null, "block cannot be null");
      checkArgument(instance.rowNumber >= 0L, "rowNumber must be greater than or equal to zero");
      return instance;
    }
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
      RowId other = (RowId) object;
      return (hash == other.hash) && (objectId == other.objectId) && (rowNumber == other.rowNumber) && equal(block, other.block);
    }
  }

  @Override
  public int compareTo(RowId other) {
    ComparisonChain chain = ComparisonChain.start();
    chain = chain.compare(objectId, other.objectId);
    chain = chain.compare(block, other.block);
    chain = chain.compare(rowNumber, other.rowNumber);
    return chain.result();
  }

  @Override
  public String toString() {
    ToStringHelper helper = toStringHelper(this);
    helper.add("objectId", objectId);
    helper.add("block", block);
    helper.add("rowNumber", rowNumber);
    return helper.toString();
  }

  public long getObjectId() {
    return objectId;
  }

  public BlockId getBlock() {
    return block;
  }

  public long getRowNumber() {
    return rowNumber;
  }

}
