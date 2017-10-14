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
    if (snapshot.getOverall().getSelect().getCount() > 0) {
      List<Object> args = newArrayList();
      args.addAll(getArgs(snapshot.getOverall().getSelect(), snapshot.getOverallWallTimeMicros()));
      args.addAll(getArgs(snapshot.getCurrent().getSelect(), snapshot.getCurrentWallTimeMicros()));
      info(LOGGER, "read---->[%s %s docs/sec cpu %s|%s] [%s %s docs/sec cpu %s|%s]", args.toArray());
    }
    if (snapshot.getOverall().getConvert().getCount() > 0) {
      List<Object> args = newArrayList();
      args.addAll(getArgs(snapshot.getOverall().getConvert(), snapshot.getOverallWallTimeMicros()));
      args.addAll(getArgs(snapshot.getCurrent().getConvert(), snapshot.getCurrentWallTimeMicros()));
      info(LOGGER, "convert->[%s %s docs/sec cpu %s|%s] [%s %s docs/sec cpu %s|%s]", args.toArray());
    }
    if (snapshot.getOverall().getUpdate().getCount() > 0) {
      List<Object> args = newArrayList();
      args.addAll(getArgs(snapshot.getOverall().getUpdate(), snapshot.getOverallWallTimeMicros()));
      args.addAll(getArgs(snapshot.getCurrent().getUpdate(), snapshot.getCurrentWallTimeMicros()));
      info(LOGGER, "write--->[%s %s docs/sec cpu %s|%s] [%s %s docs/sec cpu %s|%s]", args.toArray());
    }
  }

  private static ImmutableList<Object> getArgs(DataMetric dm, long wallTimeMicros) {
    String count = getCount(checkedCast(dm.getCount()));
    String iops = getIops(dm, wallTimeMicros);
    String time = getTime(dm.getMicroseconds() / 1000);
    String wtime = getTime(wallTimeMicros / 1000);
    return ImmutableList.<Object>of(count, iops, time, wtime);
  }

  private static String getIops(DataMetric metric, long microseconds) {
    double millis = microseconds / 1000D;
    double seconds = millis / 1000;
    double iops = metric.getCount() / seconds;
    return getCountFormatter().format(iops);
  }

}
