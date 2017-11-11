package ua.utility.kfsdbupgrade.log;

import static java.lang.String.format;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

public final class Logging {

  static final String LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS zzz";
  static final String LOG_TIME_ZONE = "America/Phoenix";

  public static void info(Logger logger, String msg, Object... args) {
    if (args == null || args.length == 0) {
      logger.info(msg);
    } else {
      logger.info(format(msg, args));
    }
  }

  public static String date(long timestamp) {
    SimpleDateFormat formatter = new SimpleDateFormat(LOG_DATE_FORMAT);
    formatter.setTimeZone(TimeZone.getTimeZone(LOG_TIME_ZONE));
    return formatter.format(new Date(timestamp));
  }

}
