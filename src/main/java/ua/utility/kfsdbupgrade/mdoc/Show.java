package ua.utility.kfsdbupgrade.mdoc;

import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCountFormatter;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import org.apache.log4j.Logger;

public final class Show {

  private static final Logger LOGGER = getLogger(Show.class);

  public static void show(DatabaseMetric snapshot) {
    String read = getIops(snapshot.getOverall().getSelect(), snapshot.getWallTimeMicros());
    String convert = getIops(snapshot.getOverall().getConvert(), snapshot.getWallTimeMicros());
    String write = getIops(snapshot.getOverall().getUpdate(), snapshot.getWallTimeMicros());
    String elapsed = getTime(snapshot.getWallTimeMicros() / 1000);
    info(LOGGER, "iops [r:%s, c:%s, w:%s] %s", read, convert, write, elapsed);
  }

  private static String getIops(DataMetric metric, long microseconds) {
    double millis = metric.getMicroseconds() / 1000D;
    double seconds = millis / 1000;
    double iops = metric.getCount() / seconds;
    return getCountFormatter().format(iops);
  }

}
