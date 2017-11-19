package ua.utility.kfsdbupgrade.rds;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

// singleton enum pattern
public enum OracleJdbcUrlFunction implements Function<OracleDatabase, String> {
  INSTANCE;

  public String apply(OracleDatabase input) {
    return Joiner.on(':').join("jdbc", "oracle", "thin", "@" + input.getEndpoint(), input.getPort(), input.getSid());
  }

}
