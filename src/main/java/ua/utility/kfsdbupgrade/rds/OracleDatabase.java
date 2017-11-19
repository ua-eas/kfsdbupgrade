package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class OracleDatabase {

  private final String name;
  private final String endpoint;
  private final String sid;
  private final int port;

  private OracleDatabase(Builder builder) {
    this.name = builder.name;
    this.endpoint = builder.endpoint;
    this.sid = builder.sid;
    this.port = builder.port;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private String endpoint;
    private String sid;
    private int port;

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withEndpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder withSid(String sid) {
      this.sid = sid;
      return this;
    }

    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    public OracleDatabase build() {
      return validate(new OracleDatabase(this));
    }

    private static OracleDatabase validate(OracleDatabase instance) {
      checkArgument(isNotBlank(instance.name), "name cannot be blank");
      checkArgument(isNotBlank(instance.endpoint), "endpoint cannot be blank");
      checkArgument(isNotBlank(instance.sid), "sid cannot be blank");
      checkArgument(instance.port > 0, "port must be greater than zero");
      return instance;
    }
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getSid() {
    return sid;
  }

  public int getPort() {
    return port;
  }

  public String getName() {
    return name;
  }

}
