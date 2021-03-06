package ua.utility.kfsdbupgrade.md;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.md.MDocErrorPredicate.MAINT_DOC_ERROR_PREFIX;

import org.apache.log4j.Logger;

import com.google.common.base.Function;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class MDocConverter implements Function<MaintDoc, MaintDoc> {

  private static final Logger LOGGER = getLogger(MDocConverter.class);

  public MDocConverter(EncryptionService encryptor, MaintainableXmlConversionService converter) {
    this.encryptor = checkNotNull(encryptor);
    this.converter = checkNotNull(converter);
  }

  private final EncryptionService encryptor;
  private final MaintainableXmlConversionService converter;

  public MaintDoc apply(MaintDoc input) {
    try {
      String decrypted = encryptor.isEnabled() ? encryptor.decrypt(input.getContent()) : input.getContent();
      String converted = converter.transformMaintainableXML(decrypted, input.getHeaderId());
      String encrypted = encryptor.isEnabled() ? encryptor.encrypt(converted) : converted;
      return MaintDoc.build(input.getRowId(), input.getHeaderId(), encrypted);
    } catch (Throwable e) {
      LOGGER.error("document conversion error -> " + input.getHeaderId(), e);
      return MaintDoc.build(input.getRowId(), MAINT_DOC_ERROR_PREFIX + input.getHeaderId(), input.getContent());
    }
  }

}
