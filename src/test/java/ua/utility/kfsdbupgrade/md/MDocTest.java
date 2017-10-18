package ua.utility.kfsdbupgrade.md;

import org.junit.Test;

public class MDocTest {

  @Test
  public void test() {
    try {
      new MDocsProvider().get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

}
