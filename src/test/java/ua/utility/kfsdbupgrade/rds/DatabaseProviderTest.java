package ua.utility.kfsdbupgrade.rds;

import java.util.Properties;

import org.junit.Test;

import ua.utility.kfsdbupgrade.md.PropertiesProvider;

public final class DatabaseProviderTest {

  @Test
  public void test() {
    try {
      Properties properties = new PropertiesProvider().get();
      String region = properties.getProperty("aws.region", "us-west-2");
      String snapshotDatabase = properties.getProperty("rds.snapshot.name");
      String instanceId = properties.getProperty("rds.name");
      ProvideDatabaseRequest request = new ProvideDatabaseRequest(region, snapshotDatabase, instanceId);
      new DatabaseProvider(request).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
