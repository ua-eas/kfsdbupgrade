package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Files.asByteSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Closer;

public final class Zips {

  private Zips() {
  }

  public static ImmutableMap<String, ByteSource> unzip(ByteSource zipped) throws IOException {
    Closer closer = Closer.create();
    Map<String, ByteSource> map = newLinkedHashMap();
    try {
      ZipInputStream zis = closer.register(new ZipInputStream(zipped.openBufferedStream()));
      ZipEntry ze = zis.getNextEntry();
      while (ze != null) {
        if (!ze.isDirectory()) {
          String name = ze.getName();
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          copy(zis, baos);
          ByteSource bytes = wrap(baos.toByteArray());
          map.put(name, bytes);
        }
        ze = zis.getNextEntry();
      }
    } catch (Throwable e) {
      closer.rethrow(e);
    } finally {
      closer.close();
    }
    return copyOf(map);
  }

  public static ByteSource zip(Iterable<File> files) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Closer closer = Closer.create();
    try {
      ZipOutputStream out = closer.register(new ZipOutputStream(baos));
      for (File file : files) {
        ZipEntry ze = new ZipEntry(file.getPath().replace("\\", "/"));
        out.putNextEntry(ze);
        InputStream in = closer.register(asByteSource(file).openBufferedStream());
        copy(in, out);
        in.close();
      }
      out.flush();
      out.finish();
    } catch (Throwable e) {
      closer.rethrow(e);
    } finally {
      closer.close();
    }
    return wrap(baos.toByteArray());
  }

}
