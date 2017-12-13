package ua.utility.kfsdbupgrade.json;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.BaseEncoding.base64;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteSource;

public final class GuavaTypesModule extends SimpleModule {

  public GuavaTypesModule() {
    this(base64());
  }

  public GuavaTypesModule(BaseEncoding encoder) {
    checkNotNull(encoder, "encoder");
    addDeserializer(ByteSource.class, new ByteSourceDeserializer(encoder));
    addSerializer(ByteSource.class, new ByteSourceSerializer(encoder));
    addSerializer(new TableSerializer());
    addSerializer(new MultimapSerializer());
  }

  private static final long serialVersionUID = 0;

}
