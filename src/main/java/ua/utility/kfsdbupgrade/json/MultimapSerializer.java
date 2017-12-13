package ua.utility.kfsdbupgrade.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Multimap;

public final class MultimapSerializer extends JsonSerializer<Multimap<?, ?>> {

	  @Override
	  public void serialize(Multimap<?, ?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
	    jgen.writeObject(value.asMap());
	  }

	  @Override
	  @SuppressWarnings({ "unchecked", "rawtypes" })
	  public Class<Multimap<?, ?>> handledType() {
	    return (Class) Multimap.class;
	  }

}
