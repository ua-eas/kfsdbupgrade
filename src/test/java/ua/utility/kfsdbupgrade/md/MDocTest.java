package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.base.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.base.Callables.getFutures;
import static ua.utility.kfsdbupgrade.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.base.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.base.Lists.concat;
import static ua.utility.kfsdbupgrade.base.Lists.distribute;
import static ua.utility.kfsdbupgrade.base.Logging.info;
import static ua.utility.kfsdbupgrade.base.Providers.fromFunction;
import static ua.utility.kfsdbupgrade.md.Closeables.closeQuietly;

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

import ua.utility.kfsdbupgrade.md.ChunkResult;
import ua.utility.kfsdbupgrade.md.ConnectionsProvider;
import ua.utility.kfsdbupgrade.md.DataMetric;
import ua.utility.kfsdbupgrade.md.MDocContext;
import ua.utility.kfsdbupgrade.md.MDocContextProvider;
import ua.utility.kfsdbupgrade.md.MDocProvider;
import ua.utility.kfsdbupgrade.md.MDocResult;
import ua.utility.kfsdbupgrade.md.MDocUpdater;
import ua.utility.kfsdbupgrade.md.MaintDoc;
import ua.utility.kfsdbupgrade.md.RowIdProvider;

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
      List<ChunkResult> chunks = newArrayList();
      for (List<String> chunk : partition(rowIds, ctx.getChunkSize())) {
        Stopwatch current = createStarted();
        chunks.add(doChunk(rds, ec2, conns, chunk, ctx));
        progress(chunks, overall, current);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(first);
      closeQuietly(conns);
    }
  }

  private void progress(Iterable<ChunkResult> chunks, Stopwatch overall, Stopwatch current) {
    String all = analyze(chunks, overall);
    String now = analyze(getLast(chunks), current);
    info(LOGGER, "all[%s] now[%s]", all, now);
  }

  private String analyze(Iterable<ChunkResult> results, Stopwatch overall) {
    long overallElapsed = overall.elapsed(MILLISECONDS);
    long readMillis = 0;
    long convertMillis = 0;
    long writeMillis = 0;
    int overallCount = 0;
    for (ChunkResult result : results) {
      overallCount += result.getCount();
      readMillis += result.getRead().getMillis();
      convertMillis += result.getConvert().getMillis();
      writeMillis += result.getWrite().getMillis();
    }
    String throughput = throughput(overallElapsed, overallCount);
    String r = throughput(readMillis, overallCount);
    String c = throughput(convertMillis, overallCount);
    String w = throughput(writeMillis, overallCount);
    return format("%s %sd/s r%s c%s w%s - %s", getCount(overallCount), throughput, r, c, w, getTime(overallElapsed));
  }

  private String analyze(ChunkResult chunk, Stopwatch current) {
    String now = throughput(current, chunk.getCount());
    String r = throughput(chunk.getRead());
    String c = chunk.getConvert().getMillis() > 0 ? " c" + throughput(chunk.getConvert()) + " " : " ";
    String w = throughput(chunk.getWrite());
    return format("%s %sd/s r%s%sw%s - %s", getCount(chunk.getCount()), now, r, c, w, getTime(current));
  }

  private ChunkResult doChunk(ExecutorService rds, ExecutorService ec2, List<Connection> conns, List<String> rowIds, MDocContext ctx) {
    MDocResult read = read(rds, conns, rowIds, ctx.getSelectSize());
    MDocResult convert = convert(ec2, read.getDocs(), ctx.getConverter());
    MDocResult write = write(rds, conns, convert.getDocs(), ctx.getBatchSize());
    return new ChunkResult(rowIds.size(), read.getMetric(), convert.getMetric(), write.getMetric());
  }

  private String throughput(Stopwatch sw, int count) {
    return getThroughputInSeconds(sw.elapsed(MILLISECONDS), count, "").trim();
  }

  private String throughput(long millis, int count) {
    return getThroughputInSeconds(millis, count, "").trim();
  }

  private String throughput(DataMetric metric) {
    return throughput(metric.getMillis(), metric.getCount());
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
        bytes += doc.getContent().length() + doc.getRowId().length() + doc.getHeaderId().length();
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
