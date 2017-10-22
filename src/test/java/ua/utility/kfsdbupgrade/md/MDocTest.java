package ua.utility.kfsdbupgrade.md;

import java.util.Properties;

import org.junit.Test;

public class MDocTest {

  @Test
  public void test() {
    try {
      Properties props = new PropertiesProvider().get();
      new MDocsProvider(props).get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
