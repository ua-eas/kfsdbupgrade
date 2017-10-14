package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCountFormatter;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

public final class Show {

  private static final Logger LOGGER = getLogger(Show.class);

  public static void show(DatabaseMetric snapshot) {
    List<Object> args = newArrayList();
    args.addAll(getArgs(snapshot.getOverall(), snapshot.getOverallWallTimeMicros()));
    args.add(getTime(snapshot.getOverallWallTimeMicros() / 1000));
    args.addAll(getArgs(snapshot.getCurrent(), snapshot.getCurrentWallTimeMicros()));
    args.add(getTime(snapshot.getCurrentWallTimeMicros() / 1000));
    info(LOGGER, "[r%s|c%s|w%s|%s] [r%s|c%s|w%s|%s]", args.toArray());
  }

  public static ImmutableList<Object> getArgs(MDocMetric metric, long micros) {
    String read = getIops(metric.getSelect(), micros);
    String convert = getIops(metric.getConvert(), micros);
    String write = getIops(metric.getUpdate(), micros);
    return ImmutableList.<Object>of(read, convert, write);
  }

  private static String getIops(DataMetric metric, long microseconds) {
    double millis = microseconds / 1000D;
    double seconds = millis / 1000;
    double iops = metric.getCount() / seconds;
    return getCountFormatter().format(iops);
  }

}
