package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.log4j.Logger.getLogger;

import org.apache.log4j.Logger;

import com.google.common.base.Function;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class MaintDocConverterFunction implements Function<MaintDoc, MaintDoc> {

  private static final Logger LOGGER = getLogger(MaintDocConverterFunction.class);

  public MaintDocConverterFunction(EncryptionService encryptor, MaintainableXmlConversionService converter) {
    this.encryptor = checkNotNull(encryptor);
    this.converter = checkNotNull(converter);
  }

  private final EncryptionService encryptor;
  private final MaintainableXmlConversionService converter;

  public MaintDoc apply(MaintDoc input) {
    try {
      String decrypted = encryptor.isEnabled() ? encryptor.decrypt(input.getContent()) : input.getContent();
      String converted = converter.transformMaintainableXML(decrypted);
      String encrypted = encryptor.isEnabled() ? encryptor.encrypt(converted) : converted;
      return MaintDoc.build(input.getId(), encrypted);
    } catch (Throwable e) {
      e.printStackTrace();
      LOGGER.error("document conversion error -> " + input.getId(), e);
      return input;
    }
  }

}
