package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;

import javax.inject.Provider;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.google.common.base.Optional;

public final class DatabaseInstanceProvider implements Provider<Optional<DBInstance>> {

  public DatabaseInstanceProvider(AmazonRDS rds, String instanceId) {
    this.rds = checkNotNull(rds);
    this.instanceId = checkNotBlank(instanceId, "instanceId");
  }

  private final AmazonRDS rds;
  private final String instanceId;

  public Optional<DBInstance> get() {
    for (DBInstance instance : rds.describeDBInstances().getDBInstances()) {
      if (instanceId.equalsIgnoreCase(instance.getDBInstanceIdentifier())) {
        return of(instance);
      }
    }
    return absent();
  }

}
