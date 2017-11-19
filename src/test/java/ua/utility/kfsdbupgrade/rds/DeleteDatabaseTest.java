package ua.utility.kfsdbupgrade.rds;

import org.junit.Test;

import com.amazonaws.services.rds.AmazonRDS;

public final class DeleteDatabaseTest {

  @Test
  public void test() {
    try {
      String name = "kfs-integrated-upgrade";
      AmazonRDS rds = new AmazonRdsProvider().get();
      new DeleteDatabaseProvider(rds, name).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
