package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;

import com.google.common.collect.ImmutableList;

public final class MDocResult {

  public MDocResult(DataMetric metric, Iterable<MaintDoc> docs) {
    this.metric = checkNotNull(metric);
    this.docs = copyOf(docs);
  }

  private final DataMetric metric;
  private final ImmutableList<MaintDoc> docs;

  public DataMetric getMetric() {
    return metric;
  }

  public ImmutableList<MaintDoc> getDocs() {
    return docs;
  }

}
