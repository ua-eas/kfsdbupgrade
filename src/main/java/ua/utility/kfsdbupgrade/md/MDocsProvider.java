package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.Math.round;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.md.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.md.MaintDocField.DOC_CNTNT;
import static ua.utility.kfsdbupgrade.md.MaintDocField.VER_NBR;
import static ua.utility.kfsdbupgrade.md.base.Callables.fromProvider;
import static ua.utility.kfsdbupgrade.md.base.Callables.getFutures;
import static ua.utility.kfsdbupgrade.md.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.md.base.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Lists.concat;
import static ua.utility.kfsdbupgrade.md.base.Lists.distribute;
import static ua.utility.kfsdbupgrade.md.base.Lists.sample;
import static ua.utility.kfsdbupgrade.md.base.Lists.transform;
import static ua.utility.kfsdbupgrade.md.base.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Providers.fromFunction;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class MDocsProvider implements Provider<Long> {

  private static final Logger LOGGER = getLogger(MDocsProvider.class);

  public Long get() {
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
      List<String> rowIds = RowIdProvider.build(first, ctx.getTable(), ctx.getMax(), ctx.getMax() / 10).get();
      warmup(ctx, rds, conns, rowIds);
      info(LOGGER, "converting %s maintenance documents", getCount(rowIds.size()));
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
    return 0L;
  }

  private void warmup(MDocContext ctx, ExecutorService rds, Iterable<Connection> conns, List<String> rowIds) {
    Stopwatch sw = createStarted();
    info(LOGGER, "warming up the %s table", ctx.getTable(), getCount(rowIds.size()));
    // Convert the Oracle row id strings into RowId objects
    List<RowId> ids = transform(rowIds, RowIdConverter.getInstance());
    // Distill the full set of rows down to get the unique set of oracle blocks containing data from this table
    // When Oracle goes to fetch the data for a row, it always reads the entire block the row resides in (usually 8 kilobytes)
    // This guarantees that every file system block containing data from this table gets touched by the Oracle runtime
    Map<BlockId, RowId> blocks = getUniqueBlocks(ids);
    info(LOGGER, "%s unique blocks detected", getCount(blocks.size()));
    // Convert the list of RowId objects (each representing a unique filesystem block) into row id strings Oracle can understand
    List<String> rows = transform(blocks.values(), RowIdConverter.getInstance().reverse());
    // Select the smallest possible field from the table to minimize network traffic while still guaranteeing that every file system block gets touched
    RowsProvider.build(rds, conns, ctx.getTable(), VER_NBR.name(), rows, SingleIntegerFunction.INSTANCE, 5000, 100, true).get();
    // Since CLOB data isn't cached by default, we'll now use high speed selects to touch 10% of the CLOBS from this table
    List<String> samples = samples(ctx, rowIds);
    // This explicitly touches 10% of the CLOBS from the table, and just as importantly, provides the Oracle runtime
    // buffering routines with a very strong hint that they need to pay attention to the CLOB data from this table
    RowsProvider.build(rds, conns, ctx.getTable(), DOC_CNTNT.name(), samples, SingleStringFunction.INSTANCE, 1000, 100, true).get();
    // The table is now warmed up and ready for action
    info(LOGGER, "warmed up the %s table [%s]", ctx.getTable(), getTime(sw));
  }

  private ImmutableList<String> samples(MDocContext ctx, List<String> rowIds) {
    if (ctx.getWarmupClobsPercent().isPresent()) {
      // grab a percentage of the clobs
      int sampleSize = checkedCast(round(rowIds.size() / (100 / ctx.getWarmupClobsPercent().get())));
      return sample(rowIds, sampleSize);
    } else {
      // grab them all
      return copyOf(rowIds);
    }
  }

  private ImmutableMap<BlockId, RowId> getUniqueBlocks(Iterable<RowId> rowIds) {
    Map<BlockId, RowId> map = newLinkedHashMap();
    for (RowId rowId : rowIds) {
      map.put(rowId.getBlock(), rowId);
    }
    return copyOf(map);
  }

  private void progress(Iterable<ChunkResult> chunks, Stopwatch overall, Stopwatch current) {
    String all = analyze(chunks, overall);
    String now = analyze(getLast(chunks), current);
    info(LOGGER, "all[%s] now[%s]", all, now);
  }

  private String analyze(Iterable<ChunkResult> chunks, Stopwatch overall) {
    long overallElapsed = overall.elapsed(MILLISECONDS);
    long readMillis = 0;
    long convertMillis = 0;
    long writeMillis = 0;
    int overallCount = 0;
    for (ChunkResult chunk : chunks) {
      overallCount += chunk.getCount();
      readMillis += chunk.getRead().getMillis();
      convertMillis += chunk.getConvert().getMillis();
      writeMillis += chunk.getWrite().getMillis();
    }
    String throughput = throughput(overallElapsed, overallCount);
    String r = throughput(readMillis, overallCount);
    String c = throughput(convertMillis, overallCount);
    String w = throughput(writeMillis, overallCount);
    return format("%s %sd/s r%s c%s w%s - %s", getCount(overallCount), throughput, r, c, w, getTime(overallElapsed));
  }

  private String analyze(ChunkResult chunk, Stopwatch current) {
    String throughput = throughput(current, chunk.getCount());
    String r = throughput(chunk.getRead());
    // if convert is set to 'identity' this is usually zero milliseconds
    String c = chunk.getConvert().getMillis() > 0 ? " c" + throughput(chunk.getConvert()) + " " : " ";
    String w = throughput(chunk.getWrite());
    return format("%s %sd/s r%s%sw%s - %s", getCount(chunk.getCount()), throughput, r, c, w, getTime(current));
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

}
