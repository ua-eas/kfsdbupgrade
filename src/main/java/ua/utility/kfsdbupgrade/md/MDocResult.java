package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;

import com.google.common.collect.ImmutableList;

public final class MDocResult {

  public MDocResult(DataMetric metric, Iterable<MaintDoc> docs) {
    this(metric, docs, ImmutableList.<String>of());
  }

  public MDocResult(DataMetric metric, Iterable<MaintDoc> docs, Iterable<String> errors) {
    this.metric = checkNotNull(metric);
    this.docs = copyOf(docs);
    this.errors = copyOf(errors);
  }

  private final DataMetric metric;
  private final ImmutableList<MaintDoc> docs;
  private final ImmutableList<String> errors;

  public DataMetric getMetric() {
    return metric;
  }

  public ImmutableList<MaintDoc> getDocs() {
    return docs;
  }

  public ImmutableList<String> getErrors() {
    return errors;
  }

}
