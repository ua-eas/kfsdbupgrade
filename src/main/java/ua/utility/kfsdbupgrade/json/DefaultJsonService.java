package ua.utility.kfsdbupgrade.json;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.copyOf;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalArgument;
import static ua.utility.kfsdbupgrade.md.base.Strings.asByteSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;

public final class DefaultJsonService extends AbstractSerializer implements JsonService {

  public DefaultJsonService() {
    this(true);
  }

  public DefaultJsonService(boolean prettyPrint) {
    this(getPrinting(prettyPrint));
  }

  public DefaultJsonService(Printing printing) {
    this(getObjectMapper(printing), printing);
  }

  @Inject
  public DefaultJsonService(ObjectMapper mapper) {
    this(mapper, Printing.MINIFIED);
  }

  public DefaultJsonService(ObjectMapper mapper, Printing printing) {
    // make a defensive copy of the mapper so its impossible for someone
    // to mess with service behavior by altering the original mapper
    this.mapper = mapper.copy();
    this.printing = printing;
  }

  // ObjectMapper is mutable, don't expose it via a getter
  private final ObjectMapper mapper;
  private final Printing printing;

  @Override
  public <T> T read(Reader reader, Class<T> type) throws IOException {
    checkNotNull(reader);
    checkNotNull(type);
    return mapper.readValue(reader, type);

  }

  @Override
  public <T> T read(InputStream in, Class<T> type) throws IOException {
    checkNotNull(in, "in");
    checkNotNull(type, "type");
    return mapper.readValue(in, type);
  }

  @Override
  public <T> void write(Writer writer, T reference) throws IOException {
    if (printing == Printing.COMPACT) {
      mapper.writer(new CompactPrettyPrinter()).writeValue(writer, reference);
    } else {
      mapper.writeValue(writer, reference);
    }
  }

  @Override
  public <T> void write(OutputStream out, T reference) throws IOException {
    if (printing == Printing.COMPACT) {
      mapper.writer(new CompactPrettyPrinter()).writeValue(out, reference);
    } else {
      mapper.writeValue(out, reference);
    }
  }

  @Override
  public <K, V> ImmutableMap<K, V> readMap(ByteSource source, Class<K> key, Class<V> value) throws IOException {
    MapLikeType type = mapper.getTypeFactory().constructMapLikeType(LinkedHashMap.class, key, value);
    Map<K, V> mutable = mapper.readValue(source.read(), type);
    return copyOf(mutable);
  }

  @Override
  public <K, V> ImmutableMap<K, V> readMap(String json, Class<K> key, Class<V> value) throws IOException {
    return readMap(asByteSource(json, UTF_8), key, value);
  }

  @Override
  public String getFileExtension() {
    return "json";
  }

  private static Printing getPrinting(boolean prettyPrinting) {
    if (prettyPrinting) {
      return Printing.PRETTY;
    } else {
      return Printing.MINIFIED;
    }
  }

  private static ObjectMapper getObjectMapper(Printing printing) {
    switch (printing) {
    case PRETTY:
      return new ObjectMapperProvider().get().configure(INDENT_OUTPUT, true);
    case COMPACT:
    case MINIFIED:
      return new ObjectMapperProvider().get();
    default:
      throw illegalArgument("%s is unknown", printing);
    }
  }

  public Printing getPrinting() {
    return printing;
  }

}
