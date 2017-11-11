package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;

import com.google.common.base.Optional;

/**
 * Strongly mimic's Guava's {@code Preconditions} class with a sensible default error message for common situations
 *
 * <pre>
 * checkArgument(StringUtils.isNotBlank(foo), &quot;'foo' cannot be blank&quot;);
 * this.foo = foo;
 * 
 * this.foo = checkNotBlank(foo, &quot;foo&quot;);
 * </pre>
 * 
 * @author Jeff Caddel
 */
public final class Preconditions {

  private Preconditions() {
  }

  private static final String NOT_NULL_MSG = "'%s' cannot be null";
  private static final String EXISTS_MSG = "[%s] does not exist";
  private static final String IS_DIR_MSG = "[%s] is not an existing directory";
  private static final String IS_FILE_MSG = "[%s] is not an existing file";
  private static final String NOT_BLANK_MSG = "'%s' cannot be blank";
  private static final String NOT_EMPTY_MSG = "'%s' cannot be the empty string";
  private static final String ARG_NAME = "argName";

  /**
   * Ensures that a File passed as an argument exists
   *
   * @param arg
   *          a File passed as an argument
   * @param argName
   *          the name of the argument
   *
   * @return the non-null File that was validated
   *
   * @throws NullPointerException
   *           If arg is null. The exception message contains the name of the argument that was null
   * @throws IllegalArgumentException
   *           If arg does not exist or argName is blank
   */
  public static File checkExists(File arg, String argName) {
    checkNotBlank(argName, ARG_NAME);
    checkNotNull(arg, argName);
    checkArgument(arg.exists(), EXISTS_MSG, arg);
    return arg;
  }

  /**
   * Ensures that a File passed as an argument is an existing directory
   *
   * @param arg
   *          a File passed as an argument
   * @param argName
   *          the name of the argument
   *
   * @return the non-null File that was validated
   *
   * @throws NullPointerException
   *           If arg is null. The exception message contains the name of the argument that was null
   * @throws IllegalArgumentException
   *           If arg does not exist, is not a directory, or argName is blank
   */
  public static File checkIsDir(File arg, String argName) {
    checkArgument(arg.isDirectory(), IS_DIR_MSG, arg);
    return arg;
  }

  /**
   * Ensures that a File passed as an argument is a normal file
   *
   * @param arg
   *          a File passed as an argument
   * @param argName
   *          the name of the argument
   *
   * @return the non-null File that was validated
   *
   * @throws NullPointerException
   *           If arg is null. The exception message contains the name of the argument that was null
   * @throws IllegalArgumentException
   *           If arg does not exist, is not a normal file, or argName is blank
   */
  public static File checkIsFile(File arg, String argName) {
    checkArgument(arg.isFile(), IS_FILE_MSG, arg);
    return arg;
  }

  /**
   * Ensures that an object reference passed as an argument is not null
   *
   * @param arg
   *          an object reference passed as an argument
   * @param argName
   *          the name of the argument
   *
   * @return the non-null object reference that was validated
   *
   * @throws NullPointerException
   *           If arg is null. The exception message contains the name of the argument that was null
   * @throws IllegalArgumentException
   *           If argName is blank
   */
  public static <T> T checkNotNull(T arg, String argName) {
    checkNotBlank(argName, ARG_NAME);
    return com.google.common.base.Preconditions.checkNotNull(arg, NOT_NULL_MSG, argName);
  }

  /**
   * Ensures that a String passed as an argument is not whitespace, empty ("") or null
   *
   * @param arg
   *          a String passed as an argument
   * @param argName
   *          the name of the argument
   *
   * @return the non-blank String that was validated
   *
   * @throws IllegalArgumentException
   *           If arg is blank. The exception message contains the name of the argument that was blank
   * @throws IllegalArgumentException
   *           If argName is blank
   */
  public static String checkNotBlank(String arg, String argName) {
    checkArgument(isNotBlank(argName), NOT_BLANK_MSG, ARG_NAME);
    checkArgument(isNotBlank(arg), NOT_BLANK_MSG, argName);
    return arg;
  }

  /**
   * Ensures that a String passed as an argument is not the empty string ("") or null
   *
   * @param arg
   *          a String passed as an argument
   * @param argName
   *          the name of the argument
   *
   * @return the non-blank String that was validated
   *
   * @throws IllegalArgumentException
   *           If arg is blank. The exception message contains the name of the argument that was blank
   * @throws IllegalArgumentException
   *           If argName is blank
   */
  public static String checkNotEmpty(String arg, String argName) {
    checkArgument(isNotBlank(argName), NOT_BLANK_MSG, ARG_NAME);
    checkArgument(isNotEmpty(arg), NOT_EMPTY_MSG, argName);
    return arg;
  }

  /**
   * Ensures that an {@code Optional<String>} passed as an argument does not contain a string that is whitespace or empty ("").
   *
   * @param arg
   *          an {@code Optional<String>} passed as an argument
   * @param argName
   *          the name of the argument
   *
   * @return the non-blank {@code Optional<String>} that was validated
   *
   * @throws IllegalArgumentException
   *           If arg is blank. The exception message contains the name of the argument that was blank
   * @throws IllegalArgumentException
   *           If argName is blank
   */
  public static Optional<String> checkNotBlank(Optional<String> arg, String argName) {
    if (arg.isPresent()) {
      checkNotBlank(arg.get(), argName);
    }
    return arg;
  }

  /**
   * Ensures that an {@code Optional<String>} passed as an argument is not the empty string ("").
   *
   * @param arg
   *          an {@code Optional<String>} passed as an argument
   * @param argName
   *          the name of the argument
   *
   * @return the non-empty string {@code Optional<String>} that was validated
   *
   * @throws IllegalArgumentException
   *           If arg is blank. The exception message contains the name of the argument that was blank
   * @throws IllegalArgumentException
   *           If argName is blank
   */
  public static Optional<String> checkNotEmpty(Optional<String> arg, String argName) {
    if (arg.isPresent()) {
      checkArgument(isNotEmpty(arg.get()), NOT_EMPTY_MSG, argName);
    }
    return arg;
  }

}
