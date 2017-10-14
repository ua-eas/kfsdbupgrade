package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MDocMetric {

  public MDocMetric(DataMetric select, DataMetric update, DataMetric convert) {
    this.select = checkNotNull(select);
    this.update = checkNotNull(update);
    this.convert = checkNotNull(convert);
  }

  private final DataMetric select;
  private final DataMetric update;
  private final DataMetric convert;

  public DataMetric getSelect() {
    return select;
  }

  public DataMetric getUpdate() {
    return update;
  }

  public DataMetric getConvert() {
    return convert;
  }

}
