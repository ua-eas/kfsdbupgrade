package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.collect.ImmutableList.copyOf;

import com.google.common.collect.ImmutableList;

public final class MDocResult {

  public MDocResult(DataMetric metric, Iterable<MaintDoc> docs) {
    this.metric = metric;
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
