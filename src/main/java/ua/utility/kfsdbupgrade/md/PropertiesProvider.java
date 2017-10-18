package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.Files.asByteSource;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import com.google.common.base.Optional;
import com.google.common.io.ByteSource;

public final class PropertiesProvider implements Provider<Properties> {

  private static final String DEFAULT_PROPS = "kfsdbupgrade.properties";
  private static final String EXTERNAL_CONFIG = "db.config";

  public PropertiesProvider() {
    this(Optional.<File>absent());
  }

  public PropertiesProvider(File external) {
    this(of(external));
  }

  public PropertiesProvider(Properties props) {
    this.provided = Optional.of(props);
    this.external = absent();
  }

  private PropertiesProvider(Optional<File> external) {
    this.external = checkNotNull(external);
    this.provided = absent();
  }

  private final Optional<File> external;
  private final Optional<Properties> provided;

  public Properties get() {
    try {
      if (external.isPresent()) {
        checkArgument(external.get().isFile(), "%s does not exist", external.get());
      }

      // get the default set of properties
      Properties props = load(wrap(asByteSource(getResource(DEFAULT_PROPS)).read()));

      if (provided.isPresent()) {
        props.putAll(provided.get());
      }

      Properties ec = getExternalConfig();
      props.putAll(ec);

      // override default properties with properties from the file (if supplied)
      if (external.isPresent()) {
        props.putAll(load(wrap(asByteSource(external.get()).read())));
      }

      // make environment variables available as properties
      Map<String, String> env = System.getenv();
      for (String key : env.keySet()) {
        String value = env.get(key);
        props.setProperty(key, value);
        props.setProperty(key.toLowerCase().replace('_', '-'), value);
        props.setProperty(key.toLowerCase().replace('_', '.'), value);
      }

      // always override everything with system properties
      props.putAll(System.getProperties());

      // return what we've got
      return props;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Properties getExternalConfig() throws IOException {
    String external = System.getProperty(EXTERNAL_CONFIG);
    if (external == null) {
      return new Properties();
    }
    File file = new File(external);
    if (file.exists()) {
      return load(asByteSource(file));
    }
    return load(asByteSource(getResource(external)));
  }

  private Properties load(ByteSource bytes) throws IOException {
    Properties props = new Properties();
    props.load(new InputStreamReader(bytes.openBufferedStream(), UTF_8));
    return props;
  }

}
