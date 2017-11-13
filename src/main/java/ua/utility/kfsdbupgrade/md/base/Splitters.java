package ua.utility.kfsdbupgrade.md.base;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public final class Splitters {

  private Splitters() {
  }

  public static ImmutableList<String> csv(CharSequence sequence) {
    return split(',', sequence);
  }

  public static ImmutableList<String> split(char delimiter, CharSequence sequence) {
    return ImmutableList.copyOf(Splitter.on(delimiter).omitEmptyStrings().trimResults().splitToList(sequence));
  }
}
