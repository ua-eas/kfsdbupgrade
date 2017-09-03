package ua.utility.kfsdbupgrade.concurrent;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.write;
import static org.apache.log4j.Logger.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Joiner;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXMLConversionServiceImpl;

public class ConcurrentTest {

  static {
    System.setProperty("log4j.debug", "false");
    System.setProperty("log4j.configuration", "log4j.properties");
  }

  private final Logger log = getLogger(ConcurrentTest.class);

  private final int maxDocumentIds = 10000;
  private final int documentIdDisplay = maxDocumentIds / 100;

  @Test
  public void test() {
    try {
      MaintenanceDocConverter mdc = getConverter();
      List<String> ids = mdc.getDocumentIds();
      // Map<String, String> original = mdc.getDocuments(ids);
      // Map<String, String> converted = mdc.convertDocuments(original);
      File output = new File("./target/ids.txt").getCanonicalFile();
      // log.info("writing " + original.size() + " to " + output);
      // StringBuilder sb = new StringBuilder();
      // for (String id : original.keySet()) {
      // sb.append(id + "," + original.get(id) + "\n");
      // }
      createParentDirs(output);
      write(Joiner.on('\n').join(ids), output, UTF_8);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private MaintenanceDocConverter getConverter() throws Exception {
    Properties props = getProperties();
    File rules = getFile(props, "maintenance-document-conversion-rules-file");
    MaintainableXMLConversionServiceImpl converter = new MaintainableXMLConversionServiceImpl(rules);
    EncryptionService encryptor = new EncryptionService(props.getProperty("encryption-key"));
    ConnectionProvider cp = new ConnectionProvider(props);
    DefaultMaintanenceDocConverter.Builder builder = DefaultMaintanenceDocConverter.builder();
    builder.withConverter(converter);
    builder.withDocumentIdDisplay(documentIdDisplay);
    builder.withEncryptor(encryptor);
    builder.withMaximumDocumentIds(maxDocumentIds);
    builder.withProvider(cp);
    return builder.build();
  }

  private Properties getProperties() throws IOException {
    File file = new File("../ua-core/src/test/resources/kfs30db.properties").getCanonicalFile();
    return new PropertiesProvider(file).get();
  }

  private File getFile(Properties props, String key) throws IOException {
    String value = props.getProperty(key);
    return new File(value).getCanonicalFile();
  }

}
