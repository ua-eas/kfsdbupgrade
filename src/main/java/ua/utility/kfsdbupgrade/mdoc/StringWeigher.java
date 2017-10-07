package ua.utility.kfsdbupgrade.mdoc;

import com.google.common.base.Function;

public enum StringWeigher implements Function<String, Long> {
  INSTANCE;

  public Long apply(String input) {
    return new Long(input.toString().length());
  }

}
