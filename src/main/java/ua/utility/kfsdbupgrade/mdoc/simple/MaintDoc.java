package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class MaintDoc {

  private final String rowId;
  private final String docId;
  private final String content;

  private MaintDoc(Builder builder) {
    this.rowId = builder.rowId;
    this.docId = builder.docId;
    this.content = builder.content;
  }

  public static MaintDoc build(String rowId, String docId, String content) {
    Builder builder = builder();
    builder.withRowId(rowId);
    builder.withDocId(docId);
    builder.withContent(content);
    return builder.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String rowId;
    private String docId;
    private String content;

    public Builder withRowId(String rowId) {
      this.rowId = rowId;
      return this;
    }

    public Builder withDocId(String docId) {
      this.docId = docId;
      return this;
    }

    public Builder withContent(String content) {
      this.content = content;
      return this;
    }

    public MaintDoc build() {
      return validate(new MaintDoc(this));
    }

    private static MaintDoc validate(MaintDoc instance) {
      checkArgument(isNotBlank(instance.rowId), "rowId may not be blank");
      checkArgument(isNotBlank(instance.docId), "docId may not be blank");
      checkArgument(isNotBlank(instance.content), "content may not be blank");
      return instance;
    }
  }

  public String getRowId() {
    return rowId;
  }

  public String getDocId() {
    return docId;
  }

  public String getContent() {
    return content;
  }

}
