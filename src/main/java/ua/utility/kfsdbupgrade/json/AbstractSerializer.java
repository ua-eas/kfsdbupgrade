package ua.utility.kfsdbupgrade.json;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Files.asByteSource;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Resources.asByteSource;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.Closer;

public abstract class AbstractSerializer implements Serializer {

  @Override
  public <T> T read(String string, Class<T> type) {
    try {
      Reader reader = new StringReader(string);
      return read(reader, type);
    } catch (IOException e) {
      throw illegalState(e, "unexpected io error");
    }
  }

  @Override
  public <T> T read(URL url, Class<T> type) throws IOException {
    return read(asByteSource(url), type);
  }

  @Override
  public <T> T read(File file, Class<T> type) throws IOException {
    return read(asByteSource(file), type);
  }

  @Override
  public <T> T read(ByteSource source, Class<T> type) throws IOException {
    checkNotNull(source);
    checkNotNull(type);
    Closer closer = Closer.create();
    try {
      InputStream in = closer.register(source.openBufferedStream());
      return read(in, type);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  @Override
  public <T> T read(CharSource source, Class<T> type) throws IOException {
    checkNotNull(source, "source");
    checkNotNull(type, "type");
    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(source.openBufferedStream());
      return read(reader, type);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  @Override
  public <T> String write(T reference) {
    try {
      StringWriter writer = new StringWriter();
      write(writer, reference);
      return writer.toString();
    } catch (IOException e) {
      throw illegalState(e, "unexpected io error");
    }
  }

  @Override
  public <T> void write(CharSink sink, T reference) throws IOException {
    Closer closer = Closer.create();
    try {
      Writer writer = closer.register(sink.openBufferedStream());
      write(writer, reference);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  @Override
  public <T> void write(ByteSink sink, T reference) throws IOException {
    Closer closer = Closer.create();
    try {
      OutputStream out = closer.register(sink.openBufferedStream());
      write(out, reference);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  @Override
  public <T> void write(File file, T reference) throws IOException {
    ByteSink sink = asByteSink(file);
    createParentDirs(file);
    write(sink, reference);
  }

}
