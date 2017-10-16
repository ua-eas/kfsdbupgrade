package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getSize;
import static ua.utility.kfsdbupgrade.mdoc.simple.Formats.getTime;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;

import ua.utility.kfsdbupgrade.log.GarbageCollectionEvent;
import ua.utility.kfsdbupgrade.log.Memory;

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

  public static void info(Logger logger, String msg, Object... args) {
    if (args == null || args.length == 0) {
      logger.info(msg);
    } else {
      logger.info(format(msg, args));
    }
  }

  private static void info(String msg, Object... args) {
    info(LOGGER, msg, args);
  }

}
