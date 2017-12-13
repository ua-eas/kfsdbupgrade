package ua.utility.kfsdbupgrade.md.base;

import com.google.common.base.Function;

public final class Xml {

  private Xml() {
  }

  public enum UnescapeFunction implements Function<String, String> {
    INSTANCE;

    public String apply(String input) {
      return unescape(input);
    }
  }

  private static String unescape(String xml) {
    xml = xml.replace("&quot;", "\"");
    xml = xml.replace("&apos;", "'");
    xml = xml.replace("&lt;", "<");
    xml = xml.replace("&gt;", ">");
    xml = xml.replace("&amp;", "&");
    return xml;
  }

}
