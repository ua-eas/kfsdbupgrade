package ua.utility.kfsdbupgrade.mdoc;

import org.apache.log4j.Logger;

public final class Closeables {

  private static final Logger LOGGER = Logger.getLogger(Closeables.class);

  public static void closeQuietly(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception e) {
        e.printStackTrace();
        LOGGER.trace("unexpected error closing a closeable " + e.getMessage());
      }
    }
  }

}
