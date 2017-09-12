package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.Files.asByteSource;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.inject.Provider;

import com.google.common.base.Optional;
import com.google.common.io.ByteSource;

public final class PropertiesProvider implements Provider<Properties> {

  private static final String DEFAULT_PROPS = "kfsdbupgrade.properties";

  public PropertiesProvider() {
    this(Optional.<File>absent());
  }

  public PropertiesProvider(File external) {
    this(of(external));
  }

  private PropertiesProvider(Optional<File> external) {
    this.external = checkNotNull(external);
  }

  private final Optional<File> external;

  public Properties get() {
    try {
      if (external.isPresent()) {
        checkArgument(external.get().isFile(), "%s does not exist", external.get());
      }

      // get the default set of properties
      Properties props = load(wrap(asByteSource(getResource(DEFAULT_PROPS)).read()));

      // override default properties with properties from the file (if supplied)
      if (external.isPresent()) {
        props.putAll(load(wrap(asByteSource(external.get()).read())));
      }

      // override everything with system properties
      props.putAll(System.getProperties());
      return props;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Properties load(ByteSource bytes) throws IOException {
    Properties props = new Properties();
    props.load(bytes.openBufferedStream());
    return props;
  }

}
