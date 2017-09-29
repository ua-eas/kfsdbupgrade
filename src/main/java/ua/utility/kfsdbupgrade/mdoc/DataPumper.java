package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.mdoc.Callables.getFutures;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getSize;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;

public final class DataPumper implements Provider<Long> {

  private static final Logger LOGGER = getLogger(DataPumper.class);

  public DataPumper(Properties props) {
    this.props = props;
  }

  private final Properties props;

  public Long get() {
    try {
      Stopwatch sw = createStarted();
      int threads = new ThreadsProvider(props).get();
      int batchSize = parseInt(props.getProperty("mdoc.batch"));
      int selectSize = parseInt(props.getProperty("mdoc.select"));
      MDocMetrics metrics = new MDocMetrics();
      ConnectionProvider provider = new ConnectionProvider(props, false);
      Optional<Integer> max = getInteger(props, "mdoc.metrics.max");
      List<String> headerIds = new HeaderIdsProvider(provider, max).get();
      List<Callable<Long>> callables = newArrayList();
      Function<MaintDoc, MaintDoc> function = RandomContentFunction.INSTANCE;
      for (List<String> distribution : distribute(headerIds, threads)) {
        // establish all connections before we start pumping
        Provider<Connection> connected = Providers.of(provider.get());
        DocConverter.Builder builder = DocConverter.builder();
        builder.withBatchSize(batchSize);
        builder.withSelectSize(selectSize);
        builder.withFunction(function);
        builder.withHeaderIds(distribution);
        builder.withMetrics(metrics);
        builder.withProvider(connected);
        DocConverter dc = builder.build();
        callables.add(fromProvider(dc));
      }
      ExecutorService executor = new ExecutorProvider("mdoc", threads).get();
      info("pumping data using %s connections, batch size %s, select size %s, cores %s", getCount(threads), getCount(batchSize), getCount(selectSize),
          getRuntime().availableProcessors());
      metrics.start();
      getFutures(executor, callables);
      new ProgressProvider(metrics, "finished").get();
      info("select --> %s", getSize(metrics.getSelect().getBytes().getValue()));
      info("update --> %s", getSize(metrics.getUpdate().getBytes().getValue()));
      info("convert -> %s", getSize(metrics.getConvert().getBytes().getValue()));
      info("elapsed -> %s", getTime(sw));
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
    return 0L;
  }

  private enum RandomContentFunction implements Function<MaintDoc, MaintDoc> {
    INSTANCE;

    private final Random random = new Random(currentTimeMillis());

    public MaintDoc apply(MaintDoc input) {
      byte[] bytes = new byte[20 * 1024];
      random.nextBytes(bytes);
      String content = new String(bytes, UTF_8);
      return MaintDoc.build(input.getDocHeaderId(), content);
    }

  }

  private Optional<Integer> getInteger(Properties props, String key) {
    String value = props.getProperty(key);
    if (value == null || "ABSENT".equals(value)) {
      return absent();
    }
    return of(parseInt(value));
  }

  private void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

}
