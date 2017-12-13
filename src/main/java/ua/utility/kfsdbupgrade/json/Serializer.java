package ua.utility.kfsdbupgrade.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;

/**
 * Serializer implementations know how to read/write java objects to/from text (typically xml or json)
 */
public interface Serializer {

  /**
   * Create an instance of {@code type} from a {@code java.lang.String}
   */
  <T> T read(String text, Class<T> type);

  /**
   * Create an instance of {@code type} from a {@code java.io.File}
   */
  <T> T read(File file, Class<T> type) throws IOException;

  /**
   * Create an instance of {@code type} from a {@code java.net.URL}
   */
  <T> T read(URL url, Class<T> type) throws IOException;

  /**
   * Create an instance of {@code type} from an {@code InputStream}
   * 
   * <p>
   * {@code InputStream} is not closed
   * </p>
   */
  <T> T read(InputStream in, Class<T> type) throws IOException;

  /**
   * Create an instance of {@code type} from a {@code Reader}
   * 
   * <p>
   * {@code Reader} is not closed
   * </p>
   */
  <T> T read(Reader in, Class<T> type) throws IOException;

  /**
   * Create an instance of {@code type} from a {@code ByteSource}
   */
  <T> T read(ByteSource source, Class<T> type) throws IOException;

  /**
   * Create an instance of {@code type} from a {@code CharSource}
   */
  <T> T read(CharSource source, Class<T> type) throws IOException;

  /**
   * Create a {@code java.lang.String} from {@code reference}
   */
  <T> String write(T reference);

  /**
   * Write {@code reference} to {@code file}
   */
  <T> void write(File file, T reference) throws IOException;

  /**
   * Write {@code reference} to {@code out}
   * 
   * <p>
   * {@code OutputStream} is not closed
   * </p>
   */
  <T> void write(OutputStream out, T reference) throws IOException;

  /**
   * Write {@code reference} to {@code out}
   * 
   * <p>
   * {@code Writer} is not closed
   * </p>
   */
  <T> void write(Writer out, T reference) throws IOException;

  /**
   * Write {@code reference} to {@code sink}
   */
  <T> void write(ByteSink sink, T reference) throws IOException;

  /**
   * Write {@code reference} to {@code sink}
   */
  <T> void write(CharSink sink, T reference) throws IOException;

  String getFileExtension();

}
