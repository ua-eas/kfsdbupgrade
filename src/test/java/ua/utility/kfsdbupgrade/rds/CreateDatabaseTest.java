package ua.utility.kfsdbupgrade.rds;

import java.util.Properties;

import org.junit.Test;

import ua.utility.kfsdbupgrade.md.PropertiesProvider;

public final class CreateDatabaseTest {

  @Test
  public void test() {
    try {
      System.setProperty("db.name", "kfs3-integrated-upgrade");
      System.setProperty("db.create", "true");
      System.setProperty("db.sid", "KFS6UPG");
      System.setProperty("db.snapshot.name", "kfs3imp");
      System.setProperty("rds.tag.contactnetid", "jcaddel");
      System.setProperty("rds.tag.ticket", "UAF-6014");
      Properties props = new PropertiesProvider().get();
      new OracleDatabaseProvider(props).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
