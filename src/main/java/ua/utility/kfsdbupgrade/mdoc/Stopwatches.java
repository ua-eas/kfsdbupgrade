package ua.utility.kfsdbupgrade.mdoc;

import com.google.common.base.Stopwatch;

public final class Stopwatches {

  public static void synchronizedStart(Stopwatch... timers) {
    for (Stopwatch timer : timers) {
      synchronized (timer) {
        if (!timer.isRunning()) {
          timer.start();
        }
      }
    }
  }

}
