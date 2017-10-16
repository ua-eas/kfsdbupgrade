package ua.utility.kfsdbupgrade;

import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

import ua.utility.kfsdbupgrade.md.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.DataPumper;

public class DataPumperTest {

  @Test
  public void test() {
    try {
      System.setProperty("log4j.configuration", "log4j.simple.properties");
      System.setProperty("upgrade.props", "/Users/jcaddel/git/ua-core/src/test/resources/kfs30db.properties");
      System.setProperty("mdoc.metrics.max", "100");
      File defaultFile = new File("./kfsdbupgrade.properties").getCanonicalFile();
      File actualFile = new File(System.getProperty("upgrade.props", defaultFile.getPath())).getCanonicalFile();
      Properties props = new PropertiesProvider(actualFile).get();
      DataPumper pumper = new DataPumper(props);
      pumper.get();
    } catch (Throwable e) {
      e.printStackTrace();
      throw illegalState(e);
    }
  }

}
