package ua.utility.kfsdbupgrade.base;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.cache.LoadingCache;

public final class Providers {

  private Providers() {
  }

  public static <T> Provider<T> of(final T instance) {
    return new ConstantProvider<T>(instance);
  }

  private static final class ConstantProvider<T> implements Provider<T> {
    private final T instance;

    private ConstantProvider(T instance) {
      this.instance = instance;
    }

    public T get() {
      return instance;
    }

    @Override
    public String toString() {
      return "of(" + instance + ")";
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof ConstantProvider) && Objects.equal(instance, ((ConstantProvider<?>) obj).instance);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(instance);
    }
  }
  
  public static <F, T> Provider<T> fromFunction(F from, Function<F, T> function) {
    return new FromFunction<F, T>(from, function);
  }

  private static class FromFunction<F, T> implements Provider<T> {

    public FromFunction(F from, Function<F, T> function) {
      this.from = checkNotNull(from, "from");
      this.function = checkNotNull(function, "function");
    }

    private final Function<F, T> function;
    private final F from;

    @Override
    public T get() {
      return function.apply(from);
    }
  }

  /**
   * Apply {@code function} to the reference returned from {@code provider}
   */
  public static <F, T> Provider<T> combine(Provider<F> provider, Function<F, T> function) {
    return new CompositeProvider<F, T>(provider, function);
  }

  private static class CompositeProvider<F, T> implements Provider<T> {

    public CompositeProvider(Provider<F> provider, Function<F, T> function) {
      this.provider = checkNotNull(provider);
      this.function = checkNotNull(function);
    }

    private final Provider<F> provider;
    private final Function<F, T> function;

    @Override
    public T get() {
      F from = provider.get();
      return function.apply(from);
    }

  }

  /**
   * Retrieve the value from {@code cache} corresponding to the {@code key} returned by {@code provider}
   */
  public static <K, V> Provider<V> combine(Provider<K> provider, LoadingCache<K, V> cache) {
    return new CacheProvider<K, V>(provider, cache);
  }

  private static class CacheProvider<K, V> implements Provider<V> {

    public CacheProvider(Provider<K> provider, LoadingCache<K, V> cache) {
      this.provider = checkNotNull(provider);
      this.cache = checkNotNull(cache);
    }

    private final Provider<K> provider;
    private final LoadingCache<K, V> cache;

    @Override
    public V get() {
      K key = provider.get();
      return cache.getUnchecked(key);
    }

  }

  public static <T> Provider<T> fromSupplier(Supplier<T> supplier) {
    return new FromSupplier<T>(supplier);
  }

  private static class FromSupplier<T> implements Provider<T> {

    public FromSupplier(Supplier<T> supplier) {
      this.supplier = checkNotNull(supplier, "supplier");
    }

    private final Supplier<T> supplier;

    @Override
    public T get() {
      return supplier.get();
    }
  }


}
