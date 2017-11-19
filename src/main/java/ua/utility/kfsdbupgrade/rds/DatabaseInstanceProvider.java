package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static ua.utility.kfsdbupgrade.rds.Rds.checkedName;

import javax.inject.Provider;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.google.common.base.Optional;

public final class DatabaseInstanceProvider implements Provider<Optional<DBInstance>> {

  public DatabaseInstanceProvider(AmazonRDS rds, String name) {
    this.rds = checkNotNull(rds);
    this.name = checkedName(name);
  }

  private final AmazonRDS rds;
  private final String name;

  public Optional<DBInstance> get() {
    for (DBInstance instance : rds.describeDBInstances().getDBInstances()) {
      if (name.equals(instance.getDBInstanceIdentifier())) {
        return of(instance);
      }
    }
    return absent();
  }

}
