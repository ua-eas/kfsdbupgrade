package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.io.ByteSource.wrap;

import java.nio.charset.Charset;

import com.google.common.io.ByteSource;

public final class Strings {

  private Strings() {
  }

  public static ByteSource asByteSource(String s, Charset charset) {
    return wrap(s.getBytes(charset));
  }

}
