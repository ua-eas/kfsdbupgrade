package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
    Stopwatch timer = createStarted();
    long display = 0;
    T instance = null;
    do {
      checkedSleep(context.getDuration(), context.getTimeout(), timer.elapsed(context.getUnit()), context.getUnit());
      display = display(display, timer);
      instance = provider.get();
    } while (!predicate.apply(instance));
    return instance;
  }

  private long display(long display, Stopwatch timer) {
    if (timer.elapsed(MILLISECONDS) - display > 60 * 1000) {
      info(LOGGER, "waited for %s, max wait=%s", getTime(timer), getTime(context.getTimeout(), context.getUnit()));
      return timer.elapsed(MILLISECONDS);
    } else {
      return display;
    }
  }

}
