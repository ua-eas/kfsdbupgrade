package ua.utility.kfsdbupgrade.json;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.BaseEncoding.base64;

import javax.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.io.BaseEncoding;

public final class ObjectMapperProvider implements Provider<ObjectMapper> {

  public ObjectMapperProvider() {
    this(base64());
  }

  public ObjectMapperProvider(BaseEncoding encoder) {
    this.encoder = checkNotNull(encoder, "encoder");
  }

  private final BaseEncoding encoder;

  /**
   * Produces a new ObjectMapper with these characteristics:
   *
   * <ul>
   * <li>registered GuavaModule</li>
   * <li>registered ByteSourceModule - uses base64 encoding</li>
   * <li>use pretty print when serializing</li>
   * <li>sort object fields alphabetically when serializing</li>
   * <li>sort map entries based on their keys when serializing</li>
   * </ul>
   */
  @Override
  public ObjectMapper get() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new GuavaTypesModule(encoder));
    mapper.configure(SORT_PROPERTIES_ALPHABETICALLY, true);
    mapper.configure(ORDER_MAP_ENTRIES_BY_KEYS, true);
    mapper.configure(INDENT_OUTPUT, true); // synonym for 'pretty print'
    mapper.configure(FAIL_ON_EMPTY_BEANS, true);
    return mapper;
  }

}
