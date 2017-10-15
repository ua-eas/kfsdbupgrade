package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Stopwatch.createUnstarted;
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
import ua.utility.kfsdbupgrade.mdoc.RowId;

public class MDocTest {

  private static final Logger LOGGER = Logger.getLogger(MDocTest.class);

  private int selected = 0;
  private int converted = 0;
  private int updated = 0;
  private final Stopwatch overall = createUnstarted();

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
      Stopwatch sw = createStarted();
      List<RowId> rowIds = getRowIds(ctx, conns.iterator().next(), ctx.getMax() / 10);
      overall.start();
      for (List<RowId> chunk : partition(rowIds, ctx.getChunkSize())) {
        List<MaintDoc> originals = select(rds, conns, chunk, ctx.getSelectSize());
        List<MaintDoc> converted = convert(ec2, originals, ctx.getConverter());
        store(rds, conns, converted, ctx.getBatchSize());
      }
      String tp = getThroughputInSeconds(sw.elapsed(MILLISECONDS), rowIds.size(), "docs/sec");
      info(LOGGER, "converted -> %s docs [%s] %s", getCount(rowIds.size()), getTime(sw), tp);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(first);
      closeQuietly(conns);
    }
  }

  private ImmutableList<MaintDoc> select(ExecutorService rds, List<Connection> conns, List<RowId> rows, int selectSize) {
    Stopwatch sw = createStarted();
    List<Callable<ImmutableList<MaintDoc>>> callables = newArrayList();
    int index = 0;
    for (List<RowId> distribution : distribute(rows, conns.size())) {
      MDocProvider mdp = new MDocProvider(conns.get(index++), distribution, selectSize);
      callables.add(fromProvider(mdp));
    }
    ImmutableList<MaintDoc> docs = concat(getFutures(rds, callables));
    String tp = getThroughputInSeconds(sw.elapsed(MILLISECONDS), docs.size(), "docs/sec");
    this.selected += docs.size();
    info(LOGGER, "selected --> %s (%s docs [%s] %s)", getCount(selected), getCount(docs.size()), getTime(sw), tp);
    return docs;
  }

  private ImmutableList<MaintDoc> convert(ExecutorService ec2, List<MaintDoc> docs, Function<MaintDoc, MaintDoc> function) {
    List<Callable<MaintDoc>> callables = newArrayList();
    for (MaintDoc doc : docs) {
      Provider<MaintDoc> provider = fromFunction(doc, function);
      Callable<MaintDoc> callable = fromProvider(provider);
      callables.add(callable);
    }
    Stopwatch sw = createStarted();
    ImmutableList<MaintDoc> converted = getFutures(ec2, callables);
    String tp = getThroughputInSeconds(sw.elapsed(MILLISECONDS), docs.size(), "docs/sec");
    this.converted += docs.size();
    info(LOGGER, "converted -> %s (%s docs [%s] %s)", getCount(this.converted), getCount(docs.size()), getTime(sw), tp);
    return converted;
  }

  private void store(ExecutorService rds, List<Connection> conns, List<MaintDoc> docs, int batchSize) {
    Stopwatch sw = createStarted();
    int index = 0;
    List<Callable<DataMetric>> callables = newArrayList();
    for (List<MaintDoc> distribution : distribute(docs, conns.size())) {
      MDocUpdater mdu = new MDocUpdater(conns.get(index++), distribution, batchSize);
      callables.add(fromProvider(mdu));
    }
    getFutures(rds, callables);
    String tp1 = getThroughputInSeconds(sw.elapsed(MILLISECONDS), docs.size(), "docs/sec");
    this.updated += docs.size();
    String tp2 = getThroughputInSeconds(overall.elapsed(MILLISECONDS), updated, "docs/sec");
    info(LOGGER, "stored ----> %s (%s docs [%s] %s) [%s]", getCount(updated), getCount(docs.size()), getTime(sw), tp1, tp2);
  }

  private ImmutableList<RowId> getRowIds(MDocContext ctx, Connection conn, int show) {
    RowIdProvider.Builder builder = RowIdProvider.builder();
    builder.withConn(conn);
    builder.withMax(ctx.getMax());
    builder.withShow(show);
    builder.withTable(ctx.getTable());
    RowIdProvider provider = builder.build();
    return provider.get();
  }

}
