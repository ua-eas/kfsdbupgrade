package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Callable;

public final class MaintDocCallable implements Callable<ConversionResult> {

  public MaintDocCallable(MaintDocFunction function, MaintDoc doc) {
    this.function = checkNotNull(function);
    this.doc = checkNotNull(doc);
  }

  private final MaintDocFunction function;
  private final MaintDoc doc;

  public ConversionResult call() {
    return function.apply(doc);
  }

}
