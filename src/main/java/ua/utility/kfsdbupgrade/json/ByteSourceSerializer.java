package ua.utility.kfsdbupgrade.json;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.BaseEncoding.base64;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteSource;

class ByteSourceSerializer extends JsonSerializer<ByteSource> {

  public ByteSourceSerializer() {
    this(base64());
  }

  public ByteSourceSerializer(BaseEncoding encoder) {
    this.encoder = checkNotNull(encoder);
  }

  private final BaseEncoding encoder;

  @Override
  public void serialize(ByteSource source, JsonGenerator jgen, SerializerProvider provider) throws IOException {
    jgen.writeString(encoder.encode(source.read()));
  }

}
