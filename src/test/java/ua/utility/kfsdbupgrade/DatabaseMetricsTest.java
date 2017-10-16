package ua.utility.kfsdbupgrade;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.base.Callables.getFutures;
import static ua.utility.kfsdbupgrade.mdoc.Show.show;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Stopwatch;

import ua.utility.kfsdbupgrade.base.Logging;
import ua.utility.kfsdbupgrade.mdoc.DatabaseMetric;
import ua.utility.kfsdbupgrade.mdoc.DatabaseMetrics;
import ua.utility.kfsdbupgrade.mdoc.ExecutorProvider;
import ua.utility.kfsdbupgrade.mdoc.ThreadsProvider;

public class DatabaseMetricsTest {

  private static final Logger LOGGER = getLogger(DatabaseMetricsTest.class);

  @Test
  public void test() {
    try {
      DatabaseMetrics metrics = new DatabaseMetrics(5, true);
      int threads = new ThreadsProvider().get();
      ExecutorService executor = new ExecutorProvider("metric", threads).get();
      List<Callable<Long>> callables = newArrayList();
      for (int i = 0; i < 6; i++) {
        callables.add(new MetricsCallable(metrics));
      }
      getFutures(executor, callables);
      Thread.sleep(300);
      DatabaseMetric snapshot = metrics.getSnapshot();
      Logging.info(LOGGER, "-- totals --");
      show(snapshot);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private static class MetricsCallable implements Callable<Long> {

    public MetricsCallable(DatabaseMetrics metrics) {
      this.metrics = metrics;
    }

    private final DatabaseMetrics metrics;
    private final Random random = new Random(currentTimeMillis());

    public Long call() {
      try {
        for (int i = 0; i < 6; i++) {
          Stopwatch select = createStarted();
          sleep(random.nextInt(100) + 100);
          metrics.select(random.nextInt(1024), select.elapsed(MICROSECONDS));
          Stopwatch convert = createStarted();
          sleep(random.nextInt(1000) + 1000);
          metrics.convert(random.nextInt(100 * 1024), convert.elapsed(MICROSECONDS));
          Stopwatch update = createStarted();
          sleep(random.nextInt(500) + 500);
          metrics.update(random.nextInt(10 * 1024), update.elapsed(MICROSECONDS));
        }
        return 0L;
      } catch (Throwable e) {
        throw new IllegalStateException(e);
      }
    }

  }

}
