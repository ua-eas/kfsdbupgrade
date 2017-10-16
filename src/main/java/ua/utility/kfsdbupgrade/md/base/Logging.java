package ua.utility.kfsdbupgrade.md.base;

import static java.lang.String.format;

import org.apache.log4j.Logger;

public final class Logging {

  public static void info(Logger logger, String msg, Object... args) {
    if (args == null || args.length == 0) {
      logger.info(msg);
    } else {
      logger.info(format(msg, args));
    }
  }

}
