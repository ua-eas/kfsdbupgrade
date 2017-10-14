package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
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
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;
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
import com.google.common.io.ByteSource;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXMLConversionServiceImpl;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;
import ua.utility.kfsdbupgrade.mdoc.simple.MDocProvider;
import ua.utility.kfsdbupgrade.mdoc.simple.MDocUpdater;
import ua.utility.kfsdbupgrade.mdoc.simple.RowIdProvider;

public class SimpleMDocConvertTest {

  private static final Logger LOGGER = Logger.getLogger(SimpleMDocConvertTest.class);

  @Test
  public void test() {
    try {
      Properties props = new PropertiesProvider().get();
      ConnectionProvider provider = new ConnectionProvider(props, false);
      int max = parseInt(props.getProperty("mdoc.max", "10987"));
      int chunkSize = parseInt(props.getProperty("mdoc.chunk", "1000"));
      int selectSize = parseInt(props.getProperty("mdoc.select", "75"));
      int batchSize = parseInt(props.getProperty("mdoc.batch", "75"));
      int rdsCores = parseInt(props.getProperty("rds.cores", "4"));
      int ec2Cores = parseInt(props.getProperty("ec2.cores", getRuntime().availableProcessors() + ""));
      ByteSource rulesXmlFile = wrap(asByteSource(getResource("MaintainableXMLUpgradeRules.xml")).read());
      MaintainableXmlConversionService service = new MaintainableXMLConversionServiceImpl(rulesXmlFile);
      String encryptionKey = props.getProperty("encryption-key");
      EncryptionService encryptor = new EncryptionService(encryptionKey);
      List<Connection> conns = openConnections(provider, rdsCores);
      List<RowId> rowIds = getRowIds(conns.iterator().next(), max);
      ExecutorService rds = new ExecutorProvider("rds", rdsCores).get();
      ExecutorService ec2 = new ExecutorProvider("ec2", ec2Cores).get();
      Stopwatch sw = createStarted();
      for (List<RowId> chunk : partition(rowIds, chunkSize)) {
        List<MaintDoc> originals = select(rds, conns, chunk, selectSize, rdsCores);
        List<MaintDoc> converted = convert(ec2, originals);
        update(rds, conns, converted, batchSize, rdsCores);
      }
      closeQuietly(conns);
      String tp = getThroughputInSeconds(sw.elapsed(MILLISECONDS), rowIds.size(), "docs/sec");
      info(LOGGER, "converted %s docs [%s] %s", getCount(rowIds.size()), getTime(sw), tp);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private void update(ExecutorService rds, List<Connection> conns, List<MaintDoc> docs, int batchSize, int rdsCores) {
    Stopwatch sw = createStarted();
    int index = 0;
    List<Callable<Long>> callables = newArrayList();
    for (List<MaintDoc> distribution : distribute(docs, rdsCores)) {
      MDocUpdater mdu = new MDocUpdater(conns.get(index++), distribution, batchSize);
      callables.add(fromProvider(mdu));
    }
    getFutures(rds, callables);
    info(LOGGER, "updated ---> %s docs [%s]", getCount(docs.size()), getTime(sw));
  }

  private ImmutableList<MaintDoc> convert(ExecutorService ec2, List<MaintDoc> docs) {
    Function<MaintDoc, MaintDoc> function = identity();
    List<Callable<MaintDoc>> callables = newArrayList();
    for (MaintDoc doc : docs) {
      Provider<MaintDoc> provider = fromFunction(doc, function);
      Callable<MaintDoc> callable = fromProvider(provider);
      callables.add(callable);
    }
    Stopwatch sw = createStarted();
    ImmutableList<MaintDoc> converted = Callables.getFutures(ec2, callables);
    info(LOGGER, "converted -> %s docs [%s]", getCount(docs.size()), getTime(sw));
    return converted;
  }

  private ImmutableList<MaintDoc> select(ExecutorService rds, List<Connection> conns, List<RowId> chunk, int selectSize, int databaseCores) {
    Stopwatch sw = createStarted();
    List<Callable<ImmutableList<MaintDoc>>> callables = newArrayList();
    int index = 0;
    for (List<RowId> distribution : distribute(chunk, databaseCores)) {
      MDocProvider mdp = new MDocProvider(conns.get(index++), distribution, selectSize);
      callables.add(fromProvider(mdp));
    }
    ImmutableList<MaintDoc> docs = concat(getFutures(rds, callables));
    info(LOGGER, "selected --> %s docs [%s]", getCount(docs.size()), getTime(sw));
    return docs;
  }

  private ImmutableList<RowId> getRowIds(Connection conn, int max) {
    Stopwatch sw = createStarted();
    info(LOGGER, "acquiring -> row ids (max of %s)", getCount(max));
    RowIdProvider provider = new RowIdProvider(conn, max);
    ImmutableList<RowId> rowIds = provider.get();
    info(LOGGER, "acquired --> %s row ids [%s]", getCount(rowIds.size()), getTime(sw));
    return rowIds;
  }

  private ImmutableList<Connection> openConnections(ConnectionProvider provider, int databaseCores) {
    List<Connection> list = newArrayList();
    for (int i = 0; i < databaseCores; i++) {
      list.add(provider.get());
    }
    return newList(list);
  }

}