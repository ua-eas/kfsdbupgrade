package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.lang.Math.max;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Threads.checkedSleep;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;

public final class Waiter<T> implements Provider<T> {

  private static final Logger LOGGER = getLogger(Waiter.class);

  public Waiter(long timeout, Provider<T> provider, Predicate<T> predicate) {
    this(new WaitContext(timeout), provider, predicate);
  }

  public Waiter(WaitContext context, Provider<T> provider, Predicate<T> predicate) {
    this.context = checkNotNull(context);
    this.provider = checkNotNull(provider);
    this.predicate = checkNotNull(predicate);
  }

  private final WaitContext context;
  private final Provider<T> provider;
  private final Predicate<T> predicate;

  @Override
  public T get() {
    Stopwatch other = createStarted();
    Stopwatch timer = createStarted();
    long display = 0;
    T instance = null;
    do {
      // if this is the first iteration, sleep for "pause" amount
      // otherwise sleep for "interval" minus the amount of time consumed by activities other than sleeping
      // if activities other than sleeping consume more time than "interval", sleep for zero
      long sleep = (instance == null) ? context.getPause() : max(context.getInterval() - other.elapsed(context.getUnit()), 0);
      checkedSleep(sleep, context.getTimeout(), timer.elapsed(context.getUnit()), context.getUnit());
      // start a new stopwatch that tracks any time spent doing anything besides sleeping
      other = createStarted();
      // display how long we've been waiting
      display = display(display, timer);
      // use the provider to obtain another instance
      instance = checkNotNull(provider.get(), "instance");
      // continue looping until we get an instance that matches our predicate
    } while (!predicate.apply(instance));
    return instance;
  }

  private long display(long display, Stopwatch timer) {
    long elapsed = timer.elapsed(context.getUnit());
    if (elapsed - display >= context.getDisplay()) {
      info(LOGGER, "waited for %s, max wait=%s", getTime(elapsed), getTime(context.getTimeout(), context.getUnit()));
      return timer.elapsed(context.getUnit());
    } else {
      return display;
    }
  }

}
