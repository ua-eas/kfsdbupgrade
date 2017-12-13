package ua.utility.kfsdbupgrade.md.base;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.alwaysFalse;
import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.io.FileWriteMode.APPEND;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.fileTreeTraverser;
import static com.google.common.io.Files.getNameWithoutExtension;
import static com.google.common.io.Files.isFile;
import static java.nio.file.Files.deleteIfExists;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalArgument;
import static ua.utility.kfsdbupgrade.md.base.Lists.sort;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.FileWriteMode;

public final class Files {

  private Files() {
  }

  private static final Predicate<File> ALWAYS_TRUE = alwaysTrue();

  public static Predicate<File> isEmptyDirPredicate() {
    return EmptyDirPredicate.INSTANCE;
  }

  private enum EmptyDirPredicate implements Predicate<File> {
    INSTANCE;

    @Override
    public boolean apply(File input) {
      return input.isDirectory() && !hasChildren(input);
    }

    private boolean hasChildren(File file) {
      File[] children = file.listFiles();
      return (children != null) && (children.length > 0);
    }

  }

  public static void write(CharSequence from, File to, Charset charset) throws IOException {
    createParentDirs(to);
    com.google.common.io.Files.write(from, to, charset);
  }

  public static void write(ByteSource bytes, File to) throws IOException {
    createParentDirs(to);
    com.google.common.io.Files.write(bytes.read(), to);
  }

  public static File canonical(File file) {
    try {
      return file.getCanonicalFile();
    } catch (IOException e) {
      throw illegalArgument(e);
    }
  }

  /**
   * Return a sorted list of all files and directories rooted at {@code dir}
   */
  public static List<File> traverse(File dir) {
    return traverse(dir, ALWAYS_TRUE);
  }

  /**
   * Return a sorted list of all files and directories rooted at {@code dir} that match {@code predicate}
   */
  public static List<File> traverse(File dir, Predicate<File> predicate) {
    return sort(filter(fileTreeTraverser().breadthFirstTraversal(dir), predicate));
  }

  public static long size(File dir) {
    return size(dir, isFile());
  }

  public static long size(File dir, Predicate<File> predicate) {
    long size = 0;
    for (File file : traverse(dir, predicate)) {
      size += file.length();
    }
    return size;
  }

  /**
   * Recursively delete all files and directories rooted at {@code file}
   */
  public static void delete(File file) throws IOException {
    if (file.isDirectory()) {
      for (File current : file.listFiles()) {
        delete(current);
      }
    }
    deleteIfExists(file.toPath());
  }

  public static URL asUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      // shouldn't happen
      throw illegalArgument(e);
    }
  }

  public static Predicate<File> fileNamePredicate(String filename) {
    return new ExactFileNamePredicate(filename);
  }

  private static class ExactFileNamePredicate implements Predicate<File> {

    public ExactFileNamePredicate(String filename) {
      this.filename = checkNotNull(filename, "filename");
    }

    private final String filename;

    @Override
    public boolean apply(File input) {
      return input.getName().equals(filename);
    }
  }

  public static Function<File, String> canonicalPath() {
    return compose(path(), CanonicalFileFunction.INSTANCE);
  }

  public static Function<File, String> path() {
    return PathFunction.INSTANCE;
  }

  private enum PathFunction implements Function<File, String> {
    INSTANCE;

    @Override
    public String apply(File input) {
      return input.getPath();
    }
  }

  public static Function<String, File> file() {
    return FileFunction.INSTANCE;
  }

  public static Function<File, File> canonicalFunction() {
    return CanonicalFileFunction.INSTANCE;
  }

  private enum FileFunction implements Function<String, File> {
    INSTANCE;

    @Override
    public File apply(String input) {
      return new File(input);
    }
  }

  private enum CanonicalFileFunction implements Function<File, File> {
    INSTANCE;

    @Override
    public File apply(File input) {
      return canonical(input);
    }
  }

  public static Function<File, String> relativePath(File parent) {
    return new RelativePathFunction(parent);
  }

  private static class RelativePathFunction implements Function<File, String> {

    public RelativePathFunction(File parent) {
      this.parent = checkNotNull(parent, "parent");
    }

    private final File parent;

    @Override
    public String apply(File input) {
      String parentPath = parent.getAbsolutePath().replace('\\', '/');
      String childPath = input.getAbsolutePath().replace('\\', '/');
      return removeStart(removeStart(childPath, parentPath), "/").replace('/', File.separatorChar);
    }
  }

  public static Predicate<File> childOf(File parent) {
    return new ChildOfPredicate(parent);
  }

  private static class ChildOfPredicate implements Predicate<File> {

    public ChildOfPredicate(File parent) {
      this.parent = checkNotNull(parent, "parent");
    }

    private final File parent;

    @Override
    public boolean apply(File input) {
      return input.getAbsolutePath().startsWith(parent.getAbsolutePath());
    }
  }

  public static Predicate<File> pathContainsAny(Iterable<String> tokens) {
    Predicate<File> predicate = alwaysFalse();
    for (String token : tokens) {
      predicate = or(predicate, pathContains(token));
    }
    return predicate;
  }

  public static Predicate<File> pathEquals(String token) {
    return new PathEqualsPredicate(token);
  }

  private static class PathEqualsPredicate implements Predicate<File> {

    public PathEqualsPredicate(String token) {
      this.token = checkNotNull(token, "token");
    }

    private final String token;

    @Override
    public boolean apply(File input) {
      return input.getPath().equals(token);
    }
  }

  public static Predicate<File> pathContains(String token) {
    return new PathContainsPredicate(token);
  }

  private static class PathContainsPredicate implements Predicate<File> {

    public PathContainsPredicate(String token) {
      this.token = checkNotNull(token, "token").replace('\\', '/');
    }

    private final String token;

    @Override
    public boolean apply(File input) {
      return input.getPath().replace('\\', '/').contains(token);
    }
  }

  public static Predicate<File> parentOf(File dir) {
    return new ParentOfPredicate(dir);
  }

  private static class ParentOfPredicate implements Predicate<File> {

    public ParentOfPredicate(File dir) {
      this.dir = checkNotNull(dir, "dir");
    }

    private final File dir;

    @Override
    public boolean apply(File input) {
      File parent = dir.getParentFile();
      while (parent != null) {
        if (parent.equals(input)) {
          return true;
        }
        parent = parent.getParentFile();
      }
      return false;
    }
  }

  /**
   * Exact same thing as Guava's method except parent directories are automatically created as needed
   */
  public static ByteSink asByteSink(File file, FileWriteMode... modes) {
    return new FileByteSink(file, modes);
  }

  private static final class FileByteSink extends ByteSink {

    private final File file;
    private final ImmutableSet<FileWriteMode> modes;

    private FileByteSink(File file, FileWriteMode... modes) {
      this.file = checkNotNull(file);
      this.modes = ImmutableSet.copyOf(modes);
    }

    @Override
    public FileOutputStream openStream() throws IOException {
      createParentDirs(file);
      return new FileOutputStream(file, modes.contains(APPEND));
    }

    @Override
    public String toString() {
      return "Files.asByteSink(" + file + ", " + modes + ")";
    }
  }

  public static File checkIsDirIfExists(File dir) {
    checkState(dir.isDirectory() || !dir.exists(), "[%s] must a directory if it exists", dir);
    return dir;
  }

  public static Optional<String> getFileExtension(String path) {
    String ext = com.google.common.io.Files.getFileExtension(path);
    if (isBlank(ext)) {
      return absent();
    } else {
      return of(ext);
    }
  }

  public static String getFileName(String path) {
    String name = getNameWithoutExtension(path);
    Optional<String> ext = getFileExtension(path);
    if (ext.isPresent()) {
      return Joiner.on('.').join(name, ext.get());
    } else {
      return name;
    }
  }
}
