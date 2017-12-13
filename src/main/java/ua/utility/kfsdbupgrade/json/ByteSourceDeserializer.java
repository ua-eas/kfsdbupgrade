package ua.utility.kfsdbupgrade.json;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.BaseEncoding.base64;
import static com.google.common.io.ByteSource.wrap;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteSource;

class ByteSourceDeserializer extends JsonDeserializer<ByteSource> {

  public ByteSourceDeserializer() {
    this(base64());
  }

  public ByteSourceDeserializer(BaseEncoding encoder) {
    this.encoder = checkNotNull(encoder);
  }

  private final BaseEncoding encoder;

  @Override
  public ByteSource deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    return wrap(encoder.decode(jp.getText()));
  }

}
