package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Callable;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class MaintDocCallable implements Callable<ConversionResult> {

  public MaintDocCallable(EncryptionService encryptor, MaintainableXmlConversionService converter, MaintDoc document) {
    this.encryptor = checkNotNull(encryptor);
    this.converter = checkNotNull(converter);
    this.document = checkNotNull(document);
  }

  private final EncryptionService encryptor;
  private final MaintainableXmlConversionService converter;
  private final MaintDoc document;

  public ConversionResult call() {
    try {
      String decrypted = encryptor.isEnabled() ? encryptor.decrypt(document.getContent()) : document.getContent();
      String converted = converter.transformMaintainableXML(decrypted, document.getDocHeaderId());
      String encrypted = encryptor.isEnabled() ? encryptor.encrypt(converted) : converted;
      return new ConversionResult(document, new MaintDoc(document.getDocHeaderId(), encrypted));
    } catch (Throwable e) {
      return new ConversionResult(document, e);
    }
  }

}
