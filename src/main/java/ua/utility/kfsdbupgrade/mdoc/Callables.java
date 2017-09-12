package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static ua.utility.kfsdbupgrade.mdoc.Exceptions.illegalArgument;
import static ua.utility.kfsdbupgrade.mdoc.Exceptions.illegalState;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;
import static ua.utility.kfsdbupgrade.mdoc.Providers.fromFunction;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

public final class Callables {

  private Callables() {}

  public static <T> T call(Callable<T> callable) {
    try {
      return callable.call();
    } catch (Exception e) {
      throw illegalArgument(e);
    }
  }

  public static <T> ImmutableList<T> call(Iterable<? extends Callable<T>> callables) {
    List<T> list = newArrayList();
    for (Callable<T> callable : callables) {
      list.add(call(callable));
    }
    return newList(list);
  }

  /**
   * Aggregate the futures returned by submitting each callable to {@code executor} and return them in an immutable list.
   */
  public static <T> ImmutableList<Future<T>> submitCallables(ExecutorService executor, Iterable<? extends Callable<T>> callables) {
    List<Future<T>> futures = newArrayList();
    for (Callable<T> callable : callables) {
      Future<T> future = executor.submit(callable);
      futures.add(future);
    }
    return copyOf(futures);
  }

  public static <T> Future<T> submitCallable(Callable<T> callable) {
    List<Callable<T>> callables = of(callable);
    List<Future<T>> futures = submitCallables(callables);
    return futures.iterator().next();
  }

  /**
   * Invoke all of the callables simultaneously.
   */
  public static <T> ImmutableList<Future<T>> submitCallables(Iterable<? extends Callable<T>> callables) {
    ExecutorService service = newFixedThreadPool(size(callables));
    return submitCallables(service, callables);
  }

  public static <T> ImmutableList<T> getFutures(ExecutorService executor, Iterable<? extends Callable<T>> callables) {
    return getFutures(submitCallables(executor, callables));
  }

  public static <T> T getFuture(Future<T> future) {
    return getUnchecked(future);
  }

  public static <T> ImmutableList<T> getFutures(Iterable<Future<T>> futures) {
    List<T> elements = newArrayList();
    for (Future<T> future : futures) {
      elements.add(getUnchecked(future));
    }
    return copyOf(elements);
  }

  private static <T> T getUnchecked(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw illegalState(e);
    } catch (ExecutionException e) {
      throw illegalState(e);
    }
  }

  public static <F, T> Function<F, Callable<T>> callable(Function<F, T> function) {
    return new CallableFunction<F, T>(function);
  }

  private static class CallableFunction<F, T> implements Function<F, Callable<T>> {

    public CallableFunction(Function<F, T> function) {
      this.function = checkNotNull(function, "function");
    }

    private final Function<F, T> function;

    @Override
    public Callable<T> apply(F input) {
      return fromProvider(fromFunction(input, function));
    }
  }

  public static <T> Callable<T> fromProvider(Provider<T> provider) {
    return new ProvidingCallable<T>(provider);
  }

  private static class ProvidingCallable<T> implements Callable<T> {

    public ProvidingCallable(Provider<T> provider) {
      this.provider = checkNotNull(provider, "provider");
    }

    private final Provider<T> provider;

    @Override
    public T call() {
      return provider.get();
    }
  }

  public static <T> Function<Future<T>, T> futureFunction() {
    return new FutureFunction<T>();
  }

  private static class FutureFunction<T> implements Function<Future<T>, T> {

    @Override
    public T apply(Future<T> input) {
      try {
        return input.get();
      } catch (ExecutionException e) {
        throw illegalState(e);
      } catch (InterruptedException e) {
        throw illegalState(e);
      }
    }
  }

  public static <T> Callable<T> constant(T instance) {
    return new ConstantCallable<T>(instance);
  }

  private static class ConstantCallable<T> implements Callable<T> {

    public ConstantCallable(T instance) {
      this.instance = checkNotNull(instance, "instance");
    }

    private final T instance;

    @Override
    public T call() {
      return instance;
    }
  }

}
