package ua.utility.kfsdbupgrade;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Callables.getFutures;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.junit.Test;

import com.google.common.base.Stopwatch;

import ua.utility.kfsdbupgrade.mdoc.DatabaseMetrics;
import ua.utility.kfsdbupgrade.mdoc.ExecutorProvider;
import ua.utility.kfsdbupgrade.mdoc.ThreadsProvider;

public class DatabaseMetricsTest {

  @Test
  public void test() {
    try {
      System.setProperty("log4j.configuration", "log4j.simple.properties");
      DatabaseMetrics metrics = new DatabaseMetrics(10000, true);
      int threads = new ThreadsProvider().get();
      ExecutorService executor = new ExecutorProvider("metric", threads).get();
      List<Callable<Long>> callables = newArrayList();
      for (int i = 0; i < 100; i++) {
        callables.add(new MetricsCallable(metrics));
      }
      getFutures(executor, callables);
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
        for (int i = 0; i < 3000; i++) {
          Stopwatch sw = createStarted();
          int next = random.nextInt(3);
          if (next == 0) {
            Thread.sleep(1);
            metrics.select(random.nextInt(25), sw.elapsed(MICROSECONDS));
          }
          if (next == 1) {
            Thread.sleep(3);
            metrics.convert(random.nextInt(25), sw.elapsed(MICROSECONDS));
          }
          if (next == 2) {
            Thread.sleep(2);
            metrics.update(random.nextInt(25), sw.elapsed(MICROSECONDS));
          }
        }
        return 0L;
      } catch (Throwable e) {
        throw new IllegalStateException(e);
      }
    }

  }

}
