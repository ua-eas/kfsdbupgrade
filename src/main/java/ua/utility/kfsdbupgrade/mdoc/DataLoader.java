package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Stopwatch.createUnstarted;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Callables.getFutures;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getThroughputInSeconds;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public class DataLoader {

  private static final Logger LOGGER = getLogger(DataLoader.class);

  public DataLoader(Properties props) {
    this.props = props;
  }

  private final Properties props;

  public Long get() {
    Stopwatch overall = createStarted();
    try {
      Stopwatch sw = createStarted();
      int threads = new ThreadsProvider(props).get();
      int batchSize = parseInt(props.getProperty("mdoc.batch"));
      int count = parseInt(props.getProperty("mdoc.gen.count", "5000"));
      int iterations = parseInt(props.getProperty("mdoc.iterations", "126"));
      List<MaintDoc> documents = getDocuments(count);
      int rows = documents.size() * iterations;
      info("generated ------> %s fake documents [%s]", getCount(documents.size()), getTime(overall));
      info("loading --------> %s fake documents", getCount(rows));
      ExecutorService executor = new ExecutorProvider("mdocs", threads).get();
      ConnectionProvider provider = new ConnectionProvider(props, false);
      List<Connection> conns = getConnections(provider, threads);
      info("established -> %s connections [%s]", getCount(conns.size()), getTime(overall));
      truncate(conns.iterator().next(), "KRNS_MAINT_DOC_T");
      info("truncated ---> KRNS_MAINT_DOC_T [%s]", getTime(overall));
      List<LoadSmallDocsCallable> callables = getCallables(documents, conns, threads, batchSize, iterations);
      info("inserting ---> %s encrypted documents [%s] using %s threads", getCount(documents.size()), getTime(overall), threads);
      getFutures(executor, callables);
      info("inserted ----> %s documents in %s [%s]", getCount(rows), getTime(sw), getThroughputInSeconds(sw, rows, "docs/second"));
      info("elapsed -----> %s", getTime(overall));
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
    return overall.elapsed(MILLISECONDS);
  }

  private ImmutableList<MaintDoc> getDocuments(int count) {
    Random random = new Random(currentTimeMillis());
    List<MaintDoc> list = newArrayList();
    byte[] bytes = new byte[20 * 1024];
    for (int i = 0; i < count; i++) {
      random.nextBytes(bytes);
      String content = new String(bytes, UTF_8);
      list.add(MaintDoc.build(Integer.toString(i + 1), content));
    }
    return newList(list);
  }

  private ImmutableList<Connection> getConnections(Provider<Connection> provider, int threads) {
    List<Connection> conns = newArrayList();
    for (int i = 0; i < threads; i++) {
      conns.add(provider.get());
    }
    return newList(conns);
  }

  private ImmutableList<LoadSmallDocsCallable> getCallables(List<MaintDoc> documents, List<Connection> conns, int threads, int batchSize, int iterations) {
    List<LoadSmallDocsCallable> callables = newArrayList();
    int index = 0;
    Counter counter = new Counter();
    Stopwatch sw = createUnstarted();
    for (List<MaintDoc> distribution : distribute(documents, threads)) {
      Connection conn = conns.get(index++);
      callables.add(new LoadSmallDocsCallable(conn, batchSize, distribution, counter, sw, iterations));
    }
    return newList(callables);
  }

  private void truncate(Connection conn, String table) throws SQLException {
    Statement stmt = null;
    try {
      stmt = conn.createStatement();
      stmt.execute("TRUNCATE TABLE " + table);
      conn.commit();
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(stmt);
    }
  }

  private void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

}
