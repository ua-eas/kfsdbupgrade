package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Ints.checkedCast;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
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
    info(LOGGER, "[r%s/%s/%s|c%s/%s|w%s/%s|%s] [r%s/%s|c%s/%s|w%s/%s|%s]", args.toArray());
  }

  public static ImmutableList<Object> getArgs(MDocMetric metric, long micros) {
    String rcount = getCount(checkedCast(metric.getSelect().getCount()));
    String read = getIops(metric.getSelect(), micros);
    String rtime = getTime(metric.getSelect().getMicroseconds() / 1000);
    String ccount = getCount(checkedCast(metric.getConvert().getCount()));
    String convert = getIops(metric.getConvert(), micros);
    String wcount = getCount(checkedCast(metric.getUpdate().getCount()));
    String write = getIops(metric.getUpdate(), micros);
    return ImmutableList.<Object>of(rcount, read, rtime, ccount, convert, wcount, write);
  }

  private static String getIops(DataMetric metric, long microseconds) {
    double millis = microseconds / 1000D;
    double seconds = millis / 1000;
    double iops = metric.getCount() / seconds;
    return getCountFormatter().format(iops);
  }

}
