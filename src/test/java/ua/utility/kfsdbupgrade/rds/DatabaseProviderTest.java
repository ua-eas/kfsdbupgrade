package ua.utility.kfsdbupgrade.rds;

import org.junit.Test;

public final class DatabaseProviderTest {

  @Test
  public void test() {
    try {
      ProvideDatabaseRequest request = new ProvideDatabaseRequest("us-west-2", "kfs3imp", "kfs36014");
      new DatabaseProvider(request).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
