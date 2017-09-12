package ua.utility.kfsdbupgrade;

import static ua.utility.kfsdbupgrade.mdoc.Exceptions.illegalState;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

import ua.utility.kfsdbupgrade.mdoc.DataPumper;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;

public class DataPumperTest {

  @Test
  public void test() {
    try {
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
