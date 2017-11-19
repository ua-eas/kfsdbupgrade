package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ua.utility.kfsdbupgrade.rds.Rds.checkedName;
import static ua.utility.kfsdbupgrade.rds.Rds.checkedSid;

import com.google.common.base.MoreObjects.ToStringHelper;

public final class OracleDatabase {

  private final String name;
  private final String endpoint;
  private final String sid;
  private final int port;

  private OracleDatabase(Builder builder) {
    this.name = checkedName(builder.name);
    this.endpoint = builder.endpoint;
    this.sid = checkedSid(builder.sid);
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

  @Override
  public String toString() {
    ToStringHelper helper = toStringHelper(this);
    helper.add("name", name);
    helper.add("endpoint", endpoint);
    helper.add("port", port);
    helper.add("sid", sid);
    return helper.toString();
  }

}
