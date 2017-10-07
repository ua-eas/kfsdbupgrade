package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = MaintDoc.Builder.class)
public final class MaintDoc {

  private final String id;
  private final String content;

  public String getId() {
    return id;
  }

  public String getContent() {
    return content;
  }

  private MaintDoc(Builder builder) {
    this.id = builder.id;
    this.content = builder.content;
  }

  public static MaintDoc build(String id, String content) {
    return new Builder().withId(id).withContent(content).build();
  }

  public static class Builder {

    private String id;
    private String content;

    public Builder withId(String id) {
      this.id = id;
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
      checkArgument(isNotBlank(instance.id), "id may not be blank");
      checkArgument(isNotBlank(instance.content), "content may not be blank");
      return instance;
    }
  }

}
