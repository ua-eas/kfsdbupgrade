package ua.utility.kfsdbupgrade.rds;

import java.util.Properties;

import org.junit.Test;

import ua.utility.kfsdbupgrade.md.PropertiesProvider;

public final class DatabaseProviderTest {

  @Test
  public void test() {
    try {
      System.setProperty("db.snapshot.name", "kfs3imp");
      System.setProperty("db.name", "kfs36014");
      Properties props = new PropertiesProvider().get();
      new DatabaseProvider(props).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
