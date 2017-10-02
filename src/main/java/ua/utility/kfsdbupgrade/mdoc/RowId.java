package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.hash;

import com.google.common.base.MoreObjects.ToStringHelper;

public final class RowId {

  private final long objectId;
  private final long fileNumber;
  private final long blockNumber;
  private final long rowNumber;
  private final int hash;

  private RowId(Builder builder) {
    this.objectId = builder.objectId;
    this.fileNumber = builder.fileNumber;
    this.blockNumber = builder.blockNumber;
    this.rowNumber = builder.rowNumber;
    this.hash = hash(objectId, fileNumber, blockNumber, rowNumber);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long objectId = -1;
    private long fileNumber = -1;
    private long blockNumber = -1;
    private long rowNumber = -1;

    public Builder withObjectId(long objectId) {
      this.objectId = objectId;
      return this;
    }

    public Builder withFileNumber(long fileNumber) {
      this.fileNumber = fileNumber;
      return this;
    }

    public Builder withBlockNumber(long blockNumber) {
      this.blockNumber = blockNumber;
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
      checkArgument(instance.fileNumber >= 0L, "fileNumber must be greater than or equal to zero");
      checkArgument(instance.blockNumber >= 0L, "blockNumber must be greater than or equal to zero");
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
      boolean equal = (hash == other.hash);
      equal = equal && (objectId == other.objectId);
      equal = equal && (fileNumber == other.fileNumber);
      equal = equal && (blockNumber == other.blockNumber);
      equal = equal && (rowNumber == other.rowNumber);
      return equal;
    }

  }

  @Override
  public String toString() {
    ToStringHelper helper = toStringHelper(this);
    helper.add("objectId", objectId);
    helper.add("fileNumber", fileNumber);
    helper.add("blockNumber", blockNumber);
    helper.add("rowNumber", rowNumber);
    return helper.toString();
  }

  public long getObjectId() {
    return objectId;
  }

  public long getFileNumber() {
    return fileNumber;
  }

  public long getBlockNumber() {
    return blockNumber;
  }

  public long getRowNumber() {
    return rowNumber;
  }

}
