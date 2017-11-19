package ua.utility.kfsdbupgrade.rds;

import com.amazonaws.services.rds.model.DBInstance;
import com.google.common.base.Function;

public enum OracleDatabaseFunction implements Function<DBInstance, OracleDatabase> {
  INSTANCE;

  public OracleDatabase apply(DBInstance input) {
    OracleDatabase.Builder builder = OracleDatabase.builder();
    builder.withName(input.getDBInstanceIdentifier());
    builder.withEndpoint(input.getEndpoint().getAddress());
    builder.withSid(input.getDBName());
    builder.withPort(input.getEndpoint().getPort());
    return builder.build();
  }

}
