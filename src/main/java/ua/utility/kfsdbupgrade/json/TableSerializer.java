package ua.utility.kfsdbupgrade.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Table;

class TableSerializer extends JsonSerializer<Table<?, ?, ?>> {

  @Override
  public void serialize(Table<?, ?, ?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeObject(value.rowMap());
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Class<Table<?, ?, ?>> handledType() {
    return (Class) Table.class;
  }
}