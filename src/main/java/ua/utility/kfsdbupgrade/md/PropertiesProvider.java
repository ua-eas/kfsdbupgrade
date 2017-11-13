package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.Files.asByteSource;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Lists.sort;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.io.ByteSource;

public final class PropertiesProvider implements Provider<Properties> {

  private static final Logger LOGGER = getLogger(PropertiesProvider.class);

  private static final String DEFAULT_PROPS = "kfsdbupgrade.properties";

  public PropertiesProvider() {
    this(Optional.<String>absent());
  }

  public PropertiesProvider(String commandLineArgument) {
    this(of(commandLineArgument));
  }

  public PropertiesProvider(Optional<String> commandLineArgument) {
    this.commandLineArgument = checkNotNull(commandLineArgument);
  }

  private final Optional<String> commandLineArgument;

  public Properties get() {
    try {
      // combine the default set of properties with any properties that have been specified via system properties, environment variables, or command line arguments
      Properties props = loadProperties(commandLineArgument);
      override(props, getEnvironmentVariables(), "environment variables");
      override(props, System.getProperties(), "system properties");
      props.putAll(getEnvironmentVariables());
      props.putAll(System.getProperties());
      return props;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Properties getEnvironmentVariables() {
    Properties props = new Properties();
    Map<String, String> env = System.getenv();
    for (String key : env.keySet()) {
      String value = env.get(key);
      props.setProperty(key, value);
      props.setProperty(key.toLowerCase().replace('_', '-'), value);
      props.setProperty(key.toLowerCase().replace('_', '.'), value);
    }
    return props;
  }

  private void override(Properties existing, Properties override, String label) {
    for (String key : sort(override.stringPropertyNames())) {
      String oldValue = existing.getProperty(key);
      String newValue = override.getProperty(key);
      if (oldValue != null && !oldValue.equals(newValue)) {
        info(LOGGER, "override -> %s from %s", key, label);
        existing.setProperty(key, newValue);
      } else {
        existing.setProperty(key, newValue);
      }
    }
  }

  private Optional<String> getEnvironmentVariable(String key) {
    String envKey1 = key.replace('.', '_').toUpperCase(ENGLISH);
    String envKey2 = key.replace('-', '_').toUpperCase(ENGLISH);
    Map<String, String> env = System.getenv();
    if (env.containsKey(envKey1)) {
      return Optional.of(env.get(envKey1));
    } else {
      return fromNullable(env.get(envKey2));
    }

  }

  private Properties loadProperties(Optional<String> commandLineArgument) throws IOException {
    Properties props = new Properties();
    props.putAll(load(DEFAULT_PROPS, "default"));
    if (commandLineArgument.isPresent()) {
      override(props, load(commandLineArgument.get(), "cli properties file"), "cli properties file");

    }
    Optional<String> env = getEnvironmentVariable(DEFAULT_PROPS);
    if (env.isPresent()) {
      override(props, load(env.get(), "environment variables"), "environment variables");
    }
    Optional<String> sys = fromNullable(System.getProperty(DEFAULT_PROPS));
    if (sys.isPresent()) {
      override(props, load(sys.get(), "system properties"), "system properties");
    }
    return props;
  }

  private Properties load(String resource, String label) throws IOException {
    File file = new File(resource);
    if (file.isFile()) {
      info(LOGGER, "loading -> %s [%s]", label, file);
      return load(asByteSource(file));
    } else {
      URL url = getResource(resource);
      info(LOGGER, "loading -> %s [%s]", label, url);
      return load(asByteSource(url));
    }
  }

  private Properties load(ByteSource bytes) throws IOException {
    Properties props = new Properties();
    props.load(new InputStreamReader(bytes.openBufferedStream(), UTF_8));
    return props;
  }

}
