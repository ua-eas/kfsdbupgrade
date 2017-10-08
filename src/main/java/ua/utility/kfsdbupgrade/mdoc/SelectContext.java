package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;

import com.google.common.collect.ImmutableList;

public final class SelectContext<T> {

  public SelectContext(Iterable<T> selected, RowSelector<T> selector) {
    this.selected = copyOf(selected);
    this.selector = checkNotNull(selector);
  }

  private final ImmutableList<T> selected;
  private final RowSelector<T> selector;

  public ImmutableList<T> getSelected() {
    return selected;
  }

  public RowSelector<T> getSelector() {
    return selector;
  }

}
