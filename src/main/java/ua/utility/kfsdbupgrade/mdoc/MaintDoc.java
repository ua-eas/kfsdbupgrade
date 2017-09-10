package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = MaintDoc.Builder.class)
public final class MaintDoc {

  private final String docHeaderId;
  private final String content;

  public String getDocHeaderId() {
    return docHeaderId;
  }

  public String getContent() {
    return content;
  }

  private MaintDoc(Builder builder) {
    this.docHeaderId = builder.docHeaderId;
    this.content = builder.content;
  }

  public static MaintDoc build(String id, String content) {
    return new Builder().withDocHeaderId(id).withContent(content).build();
  }

  public static class Builder {

    private String docHeaderId;
    private String content;

    public Builder withDocHeaderId(String docHeaderId) {
      this.docHeaderId = docHeaderId;
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
      checkArgument(!isBlank(instance.docHeaderId), "docHeaderId may not be blank");
      checkArgument(!isBlank(instance.content), "content may not be blank");
      return instance;
    }
  }

}
