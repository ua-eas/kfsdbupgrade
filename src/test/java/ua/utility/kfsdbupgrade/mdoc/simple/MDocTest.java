package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.mdoc.Callables.getFutures;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.Lists.concat;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.Providers.fromFunction;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.ExecutorProvider;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;

public class MDocTest {

  private static final Logger LOGGER = Logger.getLogger(MDocTest.class);

  @Test
  public void test() {
    List<Connection> conns = newArrayList();
    Connection first = null;
    try {
      Properties props = new PropertiesProvider().get();
      ConnectionProvider provider = new ConnectionProvider(props, false);
      first = provider.get();
      MDocContext ctx = new MDocContextProvider(props, first).get();
      ExecutorService rds = new ExecutorProvider("rds", ctx.getRdsThreads()).get();
      ExecutorService ec2 = new ExecutorProvider("ec2", ctx.getEc2Threads()).get();
      conns = new ConnectionsProvider(provider, ctx.getRdsThreads(), first).get();
      List<String> rowIds = getRowIds(ctx, conns.iterator().next(), ctx.getMax() / 10);
      Stopwatch overall = createStarted();
      int count = 0;
      for (List<String> chunk : partition(rowIds, ctx.getChunkSize())) {
        Stopwatch current = createStarted();
        MDocResult read = read(rds, conns, chunk, ctx.getSelectSize());
        MDocResult convert = convert(ec2, read.getDocs(), ctx.getConverter());
        MDocResult write = write(rds, conns, convert.getDocs(), ctx.getBatchSize());
        count += chunk.size();
        long currentElapsed = current.elapsed(MILLISECONDS);
        long overallElapsed = overall.elapsed(MILLISECONDS);
        long sumMillis = read.getMetric().getMillis() + convert.getMetric().getMillis() + write.getMetric().getMillis();
        long sumCount = read.getMetric().getCount() + convert.getMetric().getCount() + write.getMetric().getCount();
        long millisDiff = currentElapsed - sumMillis;
        long countDiff = chunk.size() - sumCount;
        String now = getThroughputInSeconds(currentElapsed, chunk.size(), "").trim();
        String throughput = getThroughputInSeconds(overallElapsed, count, "").trim();
        String r = throughput(read.getMetric());
        String c = throughput(convert.getMetric());
        String w = throughput(write.getMetric());
        info(LOGGER, "[%s %s docs/s %s] now[%s docs/s r%s c%s w%s %s]", getCount(count), throughput, getTime(overall), now, r, c, w, getTime(current));
        info(LOGGER, "millisDiff=%s countDiff=%s", millisDiff, countDiff);
        info(LOGGER, "read----->%s %s", read.getMetric().getCount(), read.getMetric().getMillis());
        info(LOGGER, "convert ->%s %s", convert.getMetric().getCount(), convert.getMetric().getMillis());
        info(LOGGER, "write---->%s %s", write.getMetric().getCount(), write.getMetric().getMillis());
        info(LOGGER, "current-->%s %s", chunk.size(), currentElapsed);
        info(LOGGER, "overall-->%s %s", count, overallElapsed);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(first);
      closeQuietly(conns);
    }
  }

  private String throughput(DataMetric metric) {
    return getThroughputInSeconds(metric.getMillis(), metric.getCount(), "").trim();
  }

  private MDocResult read(ExecutorService rds, List<Connection> conns, List<String> rows, int selectSize) {
    Stopwatch sw = createStarted();
    List<Callable<ImmutableList<MaintDoc>>> callables = newArrayList();
    int index = 0;
    for (List<String> distribution : distribute(rows, conns.size())) {
      MDocProvider mdp = new MDocProvider(conns.get(index++), distribution, selectSize);
      callables.add(fromProvider(mdp));
    }
    List<MaintDoc> docs = concat(getFutures(rds, callables));
    long bytes = sum(docs, false);
    long elapsed = sw.elapsed(MILLISECONDS);
    DataMetric metric = new DataMetric(docs.size(), bytes, elapsed);
    return new MDocResult(metric, docs);
  }

  private long sum(Iterable<MaintDoc> docs, boolean contentOnly) {
    long bytes = 0;
    for (MaintDoc doc : docs) {
      if (contentOnly) {
        bytes += doc.getContent().length();
      } else {
        bytes += doc.getRowId().length() + doc.getHeaderId().length() + doc.getContent().length();
      }
    }
    return bytes;
  }

  private MDocResult convert(ExecutorService ec2, List<MaintDoc> docs, Function<MaintDoc, MaintDoc> function) {
    Stopwatch sw = createStarted();
    List<Callable<MaintDoc>> callables = newArrayList();
    for (MaintDoc doc : docs) {
      Provider<MaintDoc> provider = fromFunction(doc, function);
      Callable<MaintDoc> callable = fromProvider(provider);
      callables.add(callable);
    }
    List<MaintDoc> converted = getFutures(ec2, callables);
    long bytes = sum(converted, true);
    long millis = sw.elapsed(MILLISECONDS);
    DataMetric metric = new DataMetric(converted.size(), bytes, millis);
    return new MDocResult(metric, converted);
  }

  private MDocResult write(ExecutorService rds, List<Connection> conns, List<MaintDoc> docs, int batchSize) {
    Stopwatch sw = createStarted();
    int index = 0;
    List<Callable<DataMetric>> callables = newArrayList();
    for (List<MaintDoc> distribution : distribute(docs, conns.size())) {
      MDocUpdater mdu = new MDocUpdater(conns.get(index++), distribution, batchSize);
      callables.add(fromProvider(mdu));
    }
    getFutures(rds, callables);
    long bytes = sum(docs, true);
    long millis = sw.elapsed(MILLISECONDS);
    DataMetric metric = new DataMetric(docs.size(), bytes, millis);
    return new MDocResult(metric, docs);
  }

  private ImmutableList<String> getRowIds(MDocContext ctx, Connection conn, int show) {
    RowIdProvider.Builder builder = RowIdProvider.builder();
    builder.withConn(conn);
    builder.withMax(ctx.getMax());
    builder.withShow(show);
    builder.withTable(ctx.getTable());
    RowIdProvider provider = builder.build();
    return provider.get();
  }

}
