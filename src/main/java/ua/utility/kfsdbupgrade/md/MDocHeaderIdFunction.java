package ua.utility.kfsdbupgrade.md;

import static org.apache.commons.lang.StringUtils.removeStart;
import static ua.utility.kfsdbupgrade.md.MDocErrorPredicate.MAINT_DOC_ERROR_PREFIX;

import com.google.common.base.Function;

public enum MDocHeaderIdFunction implements Function<MaintDoc, String> {
  INSTANCE;

  public String apply(MaintDoc input) {
    return removeStart(input.getHeaderId(), MAINT_DOC_ERROR_PREFIX);
  }

}
