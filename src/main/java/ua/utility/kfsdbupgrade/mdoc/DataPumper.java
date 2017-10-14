package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.reverse;
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
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.io.ByteSource;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXMLConversionServiceImpl;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;
import ua.utility.kfsdbupgrade.log.Logging;

public final class DataPumper implements Provider<Long> {

  private static final Logger LOGGER = getLogger(DataPumper.class);

  public DataPumper(Properties props) {
    this.props = props;
  }

  private final Properties props;

  public Long get() {
    try {
      Stopwatch sw = Logging.java();
      int threads = new ThreadsProvider(props).get();
      int batchSize = parseInt(props.getProperty("mdoc.batch"));
      int selectSize = parseInt(props.getProperty("mdoc.select"));
      boolean update = parseBoolean(props.getProperty("mdoc.update"));
      MDocMetrics metrics = new MDocMetrics();
      ConnectionProvider provider = new ConnectionProvider(props, false);
      Optional<Integer> max = getInteger(props, "mdoc.metrics.max");
      String field = props.getProperty("mdoc.field", "ROWID");
      List<String> ids = new StringProvider(provider, max, field).get();
      ByteSource rulesXmlFile = wrap(asByteSource(getResource("MaintainableXMLUpgradeRules.xml")).read());
      MaintainableXmlConversionService converter = new MaintainableXMLConversionServiceImpl(rulesXmlFile);
      EncryptionService encryptor = new EncryptionService(props.getProperty("encryption-key"));
      List<Callable<Long>> callables = newArrayList();
      Function<MaintDoc, MaintDoc> function = getFunction(props.getProperty("mdoc.content", "noop"), encryptor, converter);
      for (List<String> distribution : distribute(ids, threads)) {
        // establish all connections before we start pumping
        Provider<Connection> connected = Providers.of(provider.get());
        DocConverter.Builder builder = DocConverter.builder();
        builder.withBatchSize(batchSize);
        builder.withSelectSize(selectSize);
        builder.withFunction(function);
        builder.withHeaderIds(distribution);
        builder.withMetrics(metrics);
        builder.withProvider(connected);
        builder.withUpdate(update);
        builder.withField(field);
        DocConverter dc = builder.build();
        callables.add(fromProvider(dc));
      }
      ExecutorService executor = new ExecutorProvider("mdoc", threads).get();
      int cores = getRuntime().availableProcessors();
      info("pumping data using %s connections, batch:%s, select:%s, cores:%s update:%s", getCount(threads), getCount(batchSize), getCount(selectSize), cores, update);
      // metrics.start();
      getFutures(executor, callables);
      new ProgressProvider(metrics, "finished").get();
      info("select --> %s", getSize(metrics.getSelect().getBytes()));
      info("update --> %s", getSize(metrics.getUpdate().getBytes()));
      info("convert -> %s", getSize(metrics.getConvert().getBytes()));
      info("elapsed -> %s", getTime(sw));
      Logging.java();
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
    return 0L;
  }

  private Function<MaintDoc, MaintDoc> getFunction(String type, EncryptionService encryptor, MaintainableXmlConversionService converter) {
    if (type.equalsIgnoreCase("random")) {
      return RandomContentFunction.INSTANCE;
    }
    if (type.equalsIgnoreCase("shuffled")) {
      return ShuffledContentFunction.INSTANCE;
    }
    if (type.equalsIgnoreCase("reverse")) {
      return ReverseContentFunction.INSTANCE;
    }
    if (type.equalsIgnoreCase("convert")) {
      return new ConverterFunction(encryptor, converter);
    }
    return identity();
  }

  private static class ConverterFunction implements Function<MaintDoc, MaintDoc> {

    public ConverterFunction(EncryptionService encryptor, MaintainableXmlConversionService converter) {
      this.encryptor = checkNotNull(encryptor);
      this.converter = checkNotNull(converter);
    }

    private final EncryptionService encryptor;
    private final MaintainableXmlConversionService converter;

    public MaintDoc apply(MaintDoc input) {
      try {
        String decrypted = encryptor.decrypt(input.getContent());
        String converted = converter.transformMaintainableXML(decrypted);
        String encrypted = encryptor.encrypt(converted);
        return MaintDoc.build(input.getId(), encrypted);
      } catch (Throwable e) {
        LOGGER.error("error converting -> " + input.getId(), e);
      }
      return input;
    }

  }

  private enum ReverseContentFunction implements Function<MaintDoc, MaintDoc> {
    INSTANCE;

    public MaintDoc apply(MaintDoc input) {
      return MaintDoc.build(input.getId(), reverse(input.getContent()));
    }
  }

  private enum ShuffledContentFunction implements Function<MaintDoc, MaintDoc> {
    INSTANCE;

    public MaintDoc apply(MaintDoc input) {
      char[] chars = input.getContent().toCharArray();
      String content = new String(shuffle(chars));
      return MaintDoc.build(input.getId(), content);
    }

    private char[] shuffle(char[] chars) {
      Random rnd = ThreadLocalRandom.current();
      for (int i = chars.length - 1; i > 0; i--) {
        int index = rnd.nextInt(i + 1);
        char a = chars[index];
        chars[index] = chars[i];
        chars[i] = a;
      }
      return chars;
    }
  }

  private enum RandomContentFunction implements Function<MaintDoc, MaintDoc> {
    INSTANCE;

    public MaintDoc apply(MaintDoc input) {
      byte[] bytes = new byte[20 * 1024];
      ThreadLocalRandom.current().nextBytes(bytes);
      String content = new String(bytes, UTF_8);
      return MaintDoc.build(input.getId(), content);
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
