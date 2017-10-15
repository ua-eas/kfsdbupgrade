package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.reverse;

import java.util.Properties;

import javax.inject.Provider;

import com.google.common.base.Function;
import com.google.common.io.ByteSource;

import ua.utility.kfsdbupgrade.EncryptionService;
import ua.utility.kfsdbupgrade.MaintainableXMLConversionServiceImpl;
import ua.utility.kfsdbupgrade.MaintainableXmlConversionService;

public final class MDocFunctionProvider implements Provider<Function<MaintDoc, MaintDoc>> {

  public MDocFunctionProvider(Properties props) {
    this.props = checkNotNull(props);
  }

  private final Properties props;

  public Function<MaintDoc, MaintDoc> get() {
    String function = props.getProperty("mdoc.content", "convert");
    if (function.equalsIgnoreCase("convert")) {
      return getConverter();
    }
    if (function.equalsIgnoreCase("identity")) {
      return identity();
    } else if (function.equalsIgnoreCase("reverse")) {
      return ReverseFunction.INSTANCE;
    } else {
      throw new IllegalStateException("'" + function + "' is an unknown conversion function");
    }

  }

  private MDocConverter getConverter() {
    try {
      ByteSource rulesXmlFile = wrap(asByteSource(getResource("MaintainableXMLUpgradeRules.xml")).read());
      MaintainableXmlConversionService service = new MaintainableXMLConversionServiceImpl(rulesXmlFile);
      String encryptionKey = props.getProperty("encryption-key");
      checkState(isNotBlank(encryptionKey), "encryption key is blank");
      checkState(!encryptionKey.contains("${"), "unresolved encryption key -> %s", encryptionKey);
      EncryptionService encryptor = new EncryptionService(encryptionKey.trim());
      return new MDocConverter(encryptor, service);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private enum ReverseFunction implements Function<MaintDoc, MaintDoc> {
    INSTANCE;

    public MaintDoc apply(MaintDoc input) {
      String reversed = reverse(input.getContent());
      return MaintDoc.build(input.getRowId(), input.getHeaderId(), input.getContent());
    }
  }

}
