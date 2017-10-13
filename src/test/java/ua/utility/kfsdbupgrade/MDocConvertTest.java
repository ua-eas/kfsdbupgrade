package ua.utility.kfsdbupgrade;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Stopwatch.createUnstarted;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.lang.Runtime.getRuntime;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Callables.getFutures;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.Lists.shuffle;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;
import static ua.utility.kfsdbupgrade.mdoc.Show.show;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.DataMetrics;
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

public class MDocConvertTest {

  private static final Logger LOGGER = Logger.getLogger(MDocConvertTest.class);

  @Test
  public void test() {
    try {
      Properties props = new PropertiesProvider().get();
      ConnectionProvider provider = new ConnectionProvider(props, false);
      int threads = new ThreadsProvider(props).get();
      ExecutorService executor = new ExecutorProvider("mdoc", threads).get();
      String table = "KRNS_MAINT_DOC_T";
      int batchSize = 75;
      int max = 1000000;
      List<RowId> ids = shuffle(getRowIds(props, table, max, 50000));
      info(LOGGER, "converting %s maintanence documents using %s threads (%s cores)", getCount(ids.size()), threads, getRuntime().availableProcessors());
      int show = 1000;
      DataMetrics overall = new DataMetrics();
      DataMetrics current = new DataMetrics();
      Stopwatch timer = createUnstarted();
      Stopwatch last = createUnstarted();
      ByteSource rulesXmlFile = wrap(asByteSource(getResource("MaintainableXMLUpgradeRules.xml")).read());
      MaintainableXmlConversionService service = new MaintainableXMLConversionServiceImpl(rulesXmlFile);
      String encryptionKey = props.getProperty("encryption-key");
      EncryptionService encryptor = new EncryptionService(encryptionKey);
      RowUpdaterFunction function = new RowUpdaterFunction(show, new DataMetrics(), new DataMetrics(), createUnstarted(), createUnstarted());
      Function<MaintDoc, MaintDoc> converter = getConverter(props, service, encryptor);
      List<MaintDocCallable> callables = newArrayList();
      for (List<RowId> distribution : distribute(ids, threads)) {
        MaintDocCallable.Builder builder = MaintDocCallable.builder();
        builder.withBatchSize(batchSize);
        builder.withCurrent(current);
        builder.withFunction(function);
        builder.withLast(last);
        builder.withConverter(converter);
        builder.withOverall(overall);
        builder.withProvider(provider);
        builder.withRowIds(distribution);
        builder.withShow(show);
        builder.withTimer(timer);
        callables.add(builder.build());
      }
      getFutures(executor, callables);
      show("s", overall, current, timer, last);
      show("u", function.getOverall(), function.getCurrent(), function.getTimer(), function.getLast());
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private Function<MaintDoc, MaintDoc> getConverter(Properties props, MaintainableXmlConversionService service, EncryptionService encryptor) {
    String type = props.getProperty("mdoc.content");
    if ("convert".equals(type)) {
      return new MaintDocConverterFunction(encryptor, service);
    }
    if ("reverse".equals(type)) {
      return ReverseFunction.INSTANCE;
    }
    return identity();
  }

  private enum ReverseFunction implements Function<MaintDoc, MaintDoc> {
    INSTANCE;

    public MaintDoc apply(MaintDoc input) {
      return MaintDoc.build(input.getId(), StringUtils.reverse(input.getContent()));
    }
  }

  private ImmutableList<RowId> getRowIds(Properties props, String table, int max, int show) {
    ConnectionProvider provider = new ConnectionProvider(props, false);
    RowSelector.Builder<String> builder = RowSelector.builder();
    builder.withFunction(SingleStringFunction.INSTANCE);
    builder.withWeigher(StringWeigher.INSTANCE);
    builder.withMax(max);
    builder.withShow(show);
    builder.withTable(table);
    builder.withProvider(provider);
    RowSelector<String> selector = builder.build();
    List<String> strings = selector.get();
    return transform(strings, RowIdConverter.getInstance());
  }

}
