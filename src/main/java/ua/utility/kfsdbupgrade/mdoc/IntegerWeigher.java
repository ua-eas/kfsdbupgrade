package ua.utility.kfsdbupgrade.mdoc;

import com.google.common.base.Function;

public enum IntegerWeigher implements Function<Integer, Long> {
  INSTANCE;

  public Long apply(Integer input) {
    return 4L;
  }

}
