package ua.utility.kfsdbupgrade.mdoc;

public final class Closeables {

  public static void closeQuietly(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static void closeQuietly(Iterable<? extends AutoCloseable> closeables) {
    for (AutoCloseable closeable : closeables) {
      closeQuietly(closeable);
    }
  }

}
