package ua.utility.kfsdbupgrade;

import static com.google.common.base.Functions.identity;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.apache.commons.lang3.StringUtils.reverse;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Callables.getFutures;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.Lists.shuffle;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.DatabaseMetrics;
import ua.utility.kfsdbupgrade.mdoc.ExecutorProvider;
import ua.utility.kfsdbupgrade.mdoc.MaintDoc;
import ua.utility.kfsdbupgrade.mdoc.MaintDocConverterFunction;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.RowUpdaterFunction;
import ua.utility.kfsdbupgrade.mdoc.SingleStringFunction;
import ua.utility.kfsdbupgrade.mdoc.StringWeigher;
import ua.utility.kfsdbupgrade.mdoc.ThreadsProvider;

public class OldMDocConvertTest {

  private static final Logger LOGGER = Logger.getLogger(OldMDocConvertTest.class);

  @Test
  public void test() {
    try {
      Properties props = new PropertiesProvider().get();
      ConnectionProvider provider = new ConnectionProvider(props, false);
      int threads = new ThreadsProvider(props).get();
      ExecutorService executor = new ExecutorProvider("m", threads).get();
      String table = "KRNS_MAINT_DOC_T";
      int selectSize = 75;
      int max = 1000000;
      List<RowId> ids = shuffle(getRowIds(props, table, max, 50000));
      info(LOGGER, "converting %s maintanence documents using %s threads (%s cores)", getCount(ids.size()), threads, getRuntime().availableProcessors());
      int show = 3000;
      DatabaseMetrics metrics = new DatabaseMetrics(show, false);
      ByteSource rulesXmlFile = wrap(asByteSource(getResource("MaintainableXMLUpgradeRules.xml")).read());
      MaintainableXmlConversionService service = new MaintainableXMLConversionServiceImpl(rulesXmlFile);
      String encryptionKey = props.getProperty("encryption-key");
      EncryptionService encryptor = new EncryptionService(encryptionKey);
      RowUpdaterFunction function = new RowUpdaterFunction(metrics);
      Function<MaintDoc, MaintDoc> converter = getConverter(props, metrics, service, encryptor);
      List<MaintDocCallable> callables = newArrayList();
      for (List<RowId> distribution : distribute(ids, threads)) {
        MaintDocCallable.Builder builder = MaintDocCallable.builder();
        builder.withSelectSize(selectSize);
        builder.withFunction(function);
        builder.withConverter(converter);
        builder.withProvider(provider);
        builder.withRowIds(distribution);
        builder.withMetrics(metrics);
        callables.add(builder.build());
      }
      metrics.start();
      getFutures(executor, callables);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private Function<MaintDoc, MaintDoc> getConverter(Properties props, DatabaseMetrics metrics, MaintainableXmlConversionService service, EncryptionService encryptor) {
    String type = props.getProperty("mdoc.content");
    if ("convert".equals(type)) {
      return new MaintDocConverterFunction(metrics, encryptor, service);
    }
    if ("reverse".equals(type)) {
      return new ReverseFunction(metrics);
    }
    return identity();
  }

  private static class ReverseFunction implements Function<MaintDoc, MaintDoc> {

    public ReverseFunction(DatabaseMetrics metrics) {
      this.metrics = metrics;
    }

    private final DatabaseMetrics metrics;

    public MaintDoc apply(MaintDoc input) {
      Stopwatch sw = Stopwatch.createStarted();
      MaintDoc doc = MaintDoc.build(input.getId(), reverse(input.getContent()));
      metrics.convert(doc.getId().length() + doc.getContent().length() + 0L, sw.elapsed(MICROSECONDS));
      return doc;
    }

  }

  private ImmutableList<RowId> getRowIds(Properties props, String table, int max, int show) {
    ConnectionProvider provider = new ConnectionProvider(props, false);
    RowSelector.Builder<String> builder = RowSelector.builder();
    builder.withFunction(SingleStringFunction.INSTANCE);
    builder.withWeigher(StringWeigher.INSTANCE);
    builder.withMax(max);
    builder.withTable(table);
    builder.withProvider(provider);
    builder.withMetrics(new DatabaseMetrics(show, true));
    RowSelector<String> selector = builder.build();
    List<String> strings = selector.get();
    return transform(strings, RowIdConverter.getInstance());
  }

}
