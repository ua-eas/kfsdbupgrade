package ua.utility.kfsdbupgrade.concurrent;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.apache.log4j.Logger.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.io.ByteSource;
import com.google.common.io.Closer;
import com.google.common.io.Files;

public final class PropertiesProvider implements Provider<Properties> {

  private static final Logger LOGGER = getLogger(PropertiesProvider.class);

  public PropertiesProvider(File file) {
    this.file = checkNotNull(file);
  }

  private final File file;

  @Override
  public Properties get() {
    try {
      Properties props = load(asByteSource(getResource("kfsdbupgrade.properties")));
      props.putAll(load(Files.asByteSource(file.getCanonicalFile())));
      return props;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Properties load(ByteSource bytes) throws IOException {
    Properties properties = new Properties();
    Closer closer = Closer.create();
    try {
      LOGGER.info("loading properties -> " + file);
      Reader reader = closer.register(new InputStreamReader(bytes.openBufferedStream(), UTF_8));
      properties.load(reader);
    } catch (Throwable e) {
      closer.rethrow(e);
    } finally {
      closer.close();
    }
    return properties;
  }

}
