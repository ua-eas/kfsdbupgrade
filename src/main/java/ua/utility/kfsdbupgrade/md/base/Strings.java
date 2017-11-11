package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Predicates.alwaysFalse;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.ByteSource.wrap;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.substringsBetween;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;
import static ua.utility.kfsdbupgrade.md.base.Lists.nullSafeCopy;
import static ua.utility.kfsdbupgrade.md.base.Preconditions.checkNotBlank;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

public final class Strings {

  private Strings() {
  }

  public static String removeSubstringsBetween(String string, String open, String close) {
    List<String> tokens = nullSafeCopy(substringsBetween(string, open, close));
    String replaced = string;
    for (String token : tokens) {
      String target = open + token + close;
      replaced = replaced.replace(target, "");
    }
    return replaced;
  }

  public static Predicate<String> startsWithAny(Iterable<String> prefixes) {
    Predicate<String> predicate = alwaysFalse();
    for (String prefix : prefixes) {
      predicate = or(predicate, startsWith(prefix));
    }
    return predicate;
  }

  public static Function<String, String> prepend(String prefix) {
    return new PrependFunction(prefix);
  }

  private static class PrependFunction implements Function<String, String> {

    public PrependFunction(String prefix) {
      this.prefix = checkNotBlank(prefix, "prefix");
    }

    private final String prefix;

    @Override
    public String apply(String input) {
      return prefix + input;
    }
  }

  public static final Predicate<String> blankPredicate() {
    return BlankPredicate.INSTANCE;
  }

  private enum BlankPredicate implements Predicate<String> {
    INSTANCE;
    @Override
    public boolean apply(String input) {
      return isBlank(input);
    }
  }

  public static final String trimToNull(String s, String defaultValue) {
    String trimmed = StringUtils.trimToNull(s);
    if (trimmed == null) {
      return defaultValue;
    } else {
      return trimmed;
    }
  }

  public static final ImmutableList<String> quote(Iterable<String> strings) {
    List<String> list = newArrayList();
    for (String string : strings) {
      String element = quote(string);
      list.add(element);
    }
    return newList(list);
  }

  public static final String quote(String s) {
    return '"' + s + '"';
  }

  // Magic value used to explicitly indicate the absence of a string value
  public static final String ABSENT = "ABSENT";

  public static boolean isAbsent(String s) {
    return ABSENT.equals(s);
  }

  public static Predicate<String> absentPredicate() {
    return AbsentPredicate.INSTANCE;
  }

  private enum AbsentPredicate implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply(String input) {
      return isAbsent(input);
    }
  }

  public static boolean isNullOrAbsent(String s) {
    return (s == null) || ABSENT.equals(s);
  }

  public static String fromBlankToAbsent(String s) {
    if (isBlank(s)) {
      return ABSENT;
    } else {
      return s;
    }
  }

  public static String fromAbsentToNull(String s) {
    if (isAbsent(s)) {
      return null;
    } else {
      return s;
    }
  }

  public static boolean isNotAbsent(String s) {
    return !isAbsent(s);
  }

  public static ByteSource asByteSource(String s, Charset charset) {
    return wrap(s.getBytes(charset));
  }

  public static String flatten(String s) {
    return s.replace('\r', ' ').replace('\n', ' ');
  }

  public static String flatten(String s, String cr, String lf) {
    return s.replace("\r", cr).replace("\n", lf);
  }

  public static Predicate<String> startsWith(String prefix) {
    return new StartsWithPredicate(prefix);
  }

  private static class StartsWithPredicate implements Predicate<String> {

    public StartsWithPredicate(String prefix) {
      this.prefix = checkNotBlank(prefix, "prefix");
    }

    private final String prefix;

    @Override
    public boolean apply(String input) {
      return input.startsWith(prefix);
    }

    @Override
    public String toString() {
      return "StartsWith(" + prefix + ")";
    }

  }

  public static Predicate<String> contains(String token) {
    return new ContainsPredicate(token);
  }

  private static class ContainsPredicate implements Predicate<String> {

    public ContainsPredicate(String token) {
      this.token = checkNotBlank(token, "token");
    }

    private final String token;

    @Override
    public boolean apply(String input) {
      return input.contains(token);
    }
  }

  public static Predicate<String> endsWithAny(Iterable<String> suffixes) {
    Predicate<String> predicate = alwaysFalse();
    for (String suffix : suffixes) {
      predicate = or(predicate, endsWith(suffix));
    }
    return predicate;
  }

  public static Predicate<String> endsWith(String suffix) {
    return new EndsWithPredicate(suffix);
  }

  private static class EndsWithPredicate implements Predicate<String> {

    public EndsWithPredicate(String suffix) {
      this.suffix = checkNotBlank(suffix, "suffix");
    }

    private final String suffix;

    @Override
    public boolean apply(String input) {
      return input.endsWith(suffix);
    }
  }

  public static Predicate<String> prefixSuffix(String prefix, String suffix) {
    return and(startsWith(prefix), endsWith(suffix));
  }

  public static Function<String, String> trimFunction(String prefix, String suffix) {
    return new TrimFunction(prefix, suffix);
  }

  private static class TrimFunction implements Function<String, String> {

    public TrimFunction(String prefix, String suffix) {
      this.prefix = checkNotBlank(prefix, "prefix");
      this.suffix = checkNotBlank(suffix, "suffix");
    }

    private final String prefix;
    private final String suffix;

    @Override
    public String apply(String input) {
      return removeEnd(removeStart(input, prefix), suffix);
    }
  }

  public static Function<String, String> append(String suffix) {
    return new AppendFunction(suffix);
  }

  private static class AppendFunction implements Function<String, String> {

    public AppendFunction(String suffix) {
      this.suffix = checkNotBlank(suffix, "suffix");
    }

    private final String suffix;

    @Override
    public String apply(String input) {
      return input + suffix;
    }
  }

  public static Function<String, String> replaceFunction(String searchString, String replacement) {
    return new ReplaceFunction(searchString, replacement);
  }

  private static class ReplaceFunction implements Function<String, String> {

    public ReplaceFunction(String searchString, String replacement) {
      this.searchString = checkNotBlank(searchString, "searchString");
      this.replacement = checkNotBlank(replacement, "replacement");
    }

    private final String searchString;
    private final String replacement;

    @Override
    public String apply(String input) {
      return replace(input, searchString, replacement);
    }
  }

  public static RemoveStartFunction removeStartFunction(String prefix) {
    return new RemoveStartFunction(prefix);
  }

  private static class RemoveStartFunction implements Function<String, String> {

    public RemoveStartFunction(String prefix) {
      this.prefix = checkNotBlank(prefix, "prefix");
    }

    private final String prefix;

    @Override
    public String apply(String input) {
      return removeStart(input, prefix);
    }
  }

  private enum UpperCaseFunction implements Function<String, String> {
    INSTANCE;

    @Override
    public String apply(String input) {
      return ucase(input);
    }
  }

  public static Function<String, String> upperCaseFunction() {

    return UpperCaseFunction.INSTANCE;
  }

  private enum LowerCaseFunction implements Function<String, String> {
    INSTANCE;

    @Override
    public String apply(String input) {
      return lcase(input);
    }
  }

  public static Function<String, String> lowerCaseFunction() {

    return LowerCaseFunction.INSTANCE;
  }

  /**
   * Null-safe translation of {@code sequence} to lower case using {@code Locale.ENGLISH}
   */
  public static String lcase(CharSequence sequence) {
    if (sequence == null) {
      return null;
    } else {
      return lowerCase(sequence.toString(), ENGLISH);
    }
  }

  /**
   * Null-safe translation of {@code sequence} to upper case using {@code Locale.ENGLISH}
   */
  public static String ucase(CharSequence sequence) {
    if (sequence == null) {
      return null;
    } else {
      return upperCase(sequence.toString(), ENGLISH);
    }
  }

  /**
   * Null-safe trim of whitespace (as defined by the latest Unicode standard) from both ends of the sequence.
   * 
   * @see com.google.common.base.CharMatcher.whitespace()
   */
  public static String trim(CharSequence sequence) {
    if (sequence == null) {
      return null;
    } else {
      return whitespace().trimFrom(sequence);
    }
  }

  /**
   * Null-safe translation of carriage return + linefeed to linefeed
   */
  public static String eatcr(CharSequence sequence) {
    if (sequence == null) {
      return null;
    } else {
      return sequence.toString().replace("\r\n", "\n");
    }
  }
}
