package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static ua.utility.kfsdbupgrade.base.Lists.transform;

import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

public final class RowUpdateProvider<T> implements Provider<ImmutableList<T>> {

  public RowUpdateProvider(RowSelector<T> selector, Function<T, T> converter, Function<SelectContext<T>, RowUpdater<T>> function) {
    this.selector = checkNotNull(selector);
    this.converter = checkNotNull(converter);
    this.function = checkNotNull(function);
  }

  private final RowSelector<T> selector;
  private final Function<T, T> converter;
  private final Function<SelectContext<T>, RowUpdater<T>> function;

  public ImmutableList<T> get() {
    List<T> selected = selector.get();
    List<T> converted = transform(selected, converter);
    SelectContext<T> context = new SelectContext<>(converted, selector);
    RowUpdater<T> updater = function.apply(context);
    return updater.get();
  }

}
