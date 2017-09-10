package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class MaintDocFunction implements Function<MaintDoc, ConversionResult> {

  public MaintDocFunction(EncryptionService encryptor, MaintainableXmlConversionService converter) {
    this.encryptor = checkNotNull(encryptor);
    this.converter = checkNotNull(converter);
  }

  private final EncryptionService encryptor;
  private final MaintainableXmlConversionService converter;

  public ConversionResult apply(MaintDoc input) {
    try {
      String decrypted = encryptor.isEnabled() ? encryptor.decrypt(input.getContent()) : input.getContent();
      String converted = converter.transformMaintainableXML(decrypted);
      String encrypted = encryptor.isEnabled() ? encryptor.encrypt(converted) : converted;
      return new ConversionResult(input, MaintDoc.build(input.getDocHeaderId(), encrypted));
    } catch (Throwable e) {
      return new ConversionResult(input, e);
    }
  }

}
