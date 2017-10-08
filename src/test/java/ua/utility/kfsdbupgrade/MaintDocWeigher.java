package ua.utility.kfsdbupgrade;

import com.google.common.base.Function;

import ua.utility.kfsdbupgrade.mdoc.MaintDoc;

public enum MaintDocWeigher implements Function<MaintDoc, Long> {
  INSTANCE;

  public Long apply(MaintDoc input) {
    return 0L + input.getId().length() + input.getContent().length();
  }
}