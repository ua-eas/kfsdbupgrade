package ua.utility.kfsdbupgrade.base;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * <p>
 * Utility methods for creating exceptions with richly formatted error messages.
 *
 * <p>
 * Utility method for generating the stacktrace of an exception as a string.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * throw Exceptions.illegalArgument(&quot;port must be &gt;= %s and &lt;= %s&quot;, 0, 65535);
 * </pre>
 * 
 * @author Jeff Caddel
 */
public final class Exceptions {

  private Exceptions() {}

  public static String getStackTrace(Iterable<StackTraceElement> elements) {
    List<String> list = newArrayList();
    for (StackTraceElement element : elements) {
      list.add(format("\t at %s", element));
    }
    return Joiner.on('\n').join(list);
  }

  public static Throwable getRootCause(Throwable throwable) {
    if (throwable.getCause() == null) {
      return throwable;
    } else {
      return getRootCause(throwable.getCause());
    }
  }

  public static String getStackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  public static NullPointerException nullPointerException(String msg) {
    return new NullPointerException(msg);
  }

  public static NullPointerException nullPointerException(String msg, Object... args) {
    return new NullPointerException(format(msg, args));
  }

  public static IOException ioException(Throwable cause) {
    return new IOException(cause);
  }

  public static IOException ioException(String msg) {
    return new IOException(msg);
  }

  public static IOException ioException(String msg, Object... args) {
    return new IOException(format(msg, args));
  }

  public static IOException ioException(Throwable cause, String msg, Object... args) {
    return new IOException(format(msg, args), cause);
  }

  public static IllegalStateException illegalState(Throwable cause) {
    return new IllegalStateException(cause);
  }

  public static IllegalStateException illegalState(String msg) {
    return new IllegalStateException(msg);
  }

  public static IllegalStateException illegalState(String msg, Object... args) {
    return new IllegalStateException(format(msg, args));
  }

  public static IllegalStateException illegalState(Throwable cause, String msg, Object... args) {
    return new IllegalStateException(format(msg, args), cause);
  }

  public static IllegalArgumentException illegalArgument(Throwable cause) {
    return new IllegalArgumentException(cause);
  }

  public static IllegalArgumentException illegalArgument(String msg) {
    return new IllegalArgumentException(msg);
  }

  public static IllegalArgumentException illegalArgument(String msg, Object... args) {
    return new IllegalArgumentException(format(msg, args));
  }

  public static IllegalArgumentException illegalArgument(Throwable cause, String msg, Object... args) {
    return new IllegalArgumentException(format(msg, args), cause);
  }

  public static UnsupportedOperationException unsupportedOperation(String msg) {
    return new UnsupportedOperationException(msg);
  }

  public static UnsupportedOperationException unsupportedOperation(String msg, Object... args) {
    return new UnsupportedOperationException(format(msg, args));
  }

  public static UnsupportedOperationException unsupportedOperation(Throwable cause, String msg, Object... args) {
    return new UnsupportedOperationException(format(msg, args), cause);
  }

  private static String format(String msg, Object... args) {
    return (args == null || args.length == 0) ? msg : String.format(msg, args);
  }

  public static ExceptionsException exceptions(Iterable<? extends Throwable> throwables) {
    return new ExceptionsException(throwables);
  }

  public static class ExceptionsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final java.lang.StackTraceElement[] EMPTY_STACK_TRACE = new java.lang.StackTraceElement[0];

    public ExceptionsException(Iterable<? extends Throwable> exceptions) {
      this.exceptions = copyOf(exceptions);
    }

    private final ImmutableList<? extends Throwable> exceptions;

    public ImmutableList<? extends Throwable> getExceptions() {
      return exceptions;
    }

    @Override
    public String getMessage() {
      List<String> lines = newArrayList();
      lines.add(format("Encountered %s exceptions:", exceptions.size()));
      for (int i = 0; i < exceptions.size(); i++) {
        Throwable throwable = exceptions.get(i);
        lines.add(format("  %s of %s -> %s", (i + 1), exceptions.size(), throwable.getMessage()));
      }
      return Joiner.on('\n').join(lines);

    }

    @Override
    public String getLocalizedMessage() {
      List<String> lines = newArrayList();
      lines.add(format("Encountered %s exceptions:", exceptions.size()));
      for (int i = 0; i < exceptions.size(); i++) {
        Throwable throwable = exceptions.get(i);
        lines.add(format("  %s of %s -> %s", (i + 1), exceptions.size(), throwable.getLocalizedMessage()));
      }
      return Joiner.on('\n').join(lines);
    }

    @Override
    public synchronized Throwable getCause() {
      return exceptions.iterator().next();
    }

    @Override
    public void printStackTrace(PrintStream s) {
      s.print(toString(exceptions));
    }

    @Override
    public void printStackTrace(PrintWriter s) {
      s.write(toString(exceptions));
    }

    private static String toString(List<? extends Throwable> exceptions) {
      List<String> lines = newArrayList();
      for (int i = 0; i < exceptions.size(); i++) {
        Throwable throwable = exceptions.get(i);
        lines.add(format("Exception %s of %s: ", (i + 1), exceptions.size()));
        lines.add(Exceptions.getStackTrace(throwable));
      }
      return Joiner.on('\n').join(lines);
    }

    @Override
    public String toString() {
      return toStringHelper(this).add("throwables", exceptions).toString();
    }

    @Override
    public java.lang.StackTraceElement[] getStackTrace() {
      List<java.lang.StackTraceElement> list = newArrayList();
      for (Throwable throwable : exceptions) {
        list.addAll(copyOf(throwable.getStackTrace()));
      }
      return list.toArray(EMPTY_STACK_TRACE);
    }

  }

}
