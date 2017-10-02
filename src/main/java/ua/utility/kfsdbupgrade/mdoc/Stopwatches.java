package ua.utility.kfsdbupgrade.mdoc;

import com.google.common.base.Stopwatch;

public final class Stopwatches {

  public static Stopwatch synchronizedStart(Stopwatch sw) {
    synchronized (sw) {
      return sw.isRunning() ? sw : sw.start();
    }

  }

}
