package ua.utility.kfsdbupgrade.json;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;

public interface JsonService extends Serializer {

  <K, V> ImmutableMap<K, V> readMap(ByteSource source, Class<K> key, Class<V> value) throws IOException;

  <K, V> ImmutableMap<K, V> readMap(String json, Class<K> key, Class<V> value) throws IOException;

}
