package ua.utility.kfsdbupgrade.log;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getSize;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;

import ua.utility.kfsdbupgrade.mdoc.GarbageCollectionEvent;
import ua.utility.kfsdbupgrade.mdoc.Memory;

public final class Logging {

  private static final Logger LOGGER = getLogger(Logging.class);

  public static Stopwatch java() {
    Memory m = Memory.build();
    List<GarbageCollectionEvent> gcs = GarbageCollectionEvent.buildList();
    info("memory --> used:%s free:%s", getSize(m.getUsed()), getSize(m.getFree()));
    for (GarbageCollectionEvent gc : gcs) {
      String c = gc.getCount().isPresent() ? getCount(checkedCast(gc.getCount().get())) : "";
      String t = gc.getMillis().isPresent() ? getTime(gc.getMillis().get()) : "";
      info("gcs -----> %s: %s %s", gc.getName(), c, t);
    }
    return createStarted();
  }

  private static void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

}
