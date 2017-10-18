package ua.utility.kfsdbupgrade.md;

import com.google.common.base.Predicate;

public enum MDocErrorPredicate implements Predicate<MaintDoc> {
  INSTANCE;

  public static final String MAINT_DOC_ERROR_PREFIX = "ERR:";

  public boolean apply(MaintDoc input) {
    return input.getHeaderId().startsWith(MAINT_DOC_ERROR_PREFIX);
  }

}
