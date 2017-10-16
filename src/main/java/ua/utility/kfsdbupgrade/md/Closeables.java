package ua.utility.kfsdbupgrade.md;

import static org.apache.log4j.Logger.getLogger;

import org.apache.log4j.Logger;

public final class Closeables {

  private static final Logger LOGGER = getLogger(Closeables.class);

  public static void closeQuietly(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception e) {
        e.printStackTrace();
        LOGGER.error("unexpected closing error", e);
      }
    }
  }

  public static void closeQuietly(Iterable<? extends AutoCloseable> closeables) {
    for (AutoCloseable closeable : closeables) {
      closeQuietly(closeable);
    }
  }

}
