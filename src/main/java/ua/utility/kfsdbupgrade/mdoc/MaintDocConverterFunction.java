package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.apache.log4j.Logger.getLogger;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class MaintDocConverterFunction implements Function<MaintDoc, MaintDoc> {

  private static final Logger LOGGER = getLogger(MaintDocConverterFunction.class);

  public MaintDocConverterFunction(DatabaseMetrics metrics, EncryptionService encryptor, MaintainableXmlConversionService converter) {
    this.encryptor = checkNotNull(encryptor);
    this.converter = checkNotNull(converter);
    this.metrics = checkNotNull(metrics);
  }

  private final EncryptionService encryptor;
  private final MaintainableXmlConversionService converter;
  private final DatabaseMetrics metrics;

  public MaintDoc apply(MaintDoc input) {
    Stopwatch sw = createStarted();
    try {
      String decrypted = encryptor.isEnabled() ? encryptor.decrypt(input.getContent()) : input.getContent();
      String converted = converter.transformMaintainableXML(decrypted);
      String encrypted = encryptor.isEnabled() ? encryptor.encrypt(converted) : converted;
      MaintDoc doc = MaintDoc.build(input.getId(), encrypted);
      metrics.convert(doc.getId().length() + doc.getContent().length() + 0L, sw.elapsed(MICROSECONDS));
      return doc;
    } catch (Throwable e) {
      e.printStackTrace();
      LOGGER.error("document conversion error -> " + input.getId(), e);
      metrics.convert(input.getId().length() + input.getContent().length() + 0L, sw.elapsed(MICROSECONDS));
      return input;
    }
  }

}
