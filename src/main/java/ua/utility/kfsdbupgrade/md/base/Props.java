package ua.utility.kfsdbupgrade.md.base;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalArgument;
import static ua.utility.kfsdbupgrade.md.base.Lists.nullSafeCast;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;

import java.util.Properties;

public final class Props {

  private Props() {
  }

  public static String checkedValue(Properties props, String key) {
    return checkNotBlank(props.getProperty(key), key);
  }

  public static String checkedValue(Properties props, String... keys) {
    for (String key : keys) {
      String value = props.getProperty(key);
      if (isNotBlank(value)) {
        return value;
      }
    }
    throw illegalArgument("%s were all blank", nullSafeCast(keys, String.class));
  }

}
