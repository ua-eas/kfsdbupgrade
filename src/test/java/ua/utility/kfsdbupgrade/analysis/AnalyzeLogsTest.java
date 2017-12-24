package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.StandardSystemProperty.USER_HOME;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableTable.copyOf;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newTreeMap;
import static com.google.common.io.ByteSource.wrap;
import static com.google.common.io.Files.asByteSource;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.isFile;
import static com.google.common.io.Files.write;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.substringsBetween;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.log.Logging.logTableToStdOut;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;
import static ua.utility.kfsdbupgrade.md.base.Files.canonical;
import static ua.utility.kfsdbupgrade.md.base.Formats.getCount;
import static ua.utility.kfsdbupgrade.md.base.Formats.getSize;
import static ua.utility.kfsdbupgrade.md.base.Formats.getTime;
import static ua.utility.kfsdbupgrade.md.base.Lists.filter;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;
import static ua.utility.kfsdbupgrade.md.base.Lists.sort;
import static ua.utility.kfsdbupgrade.md.base.Zips.unzip;
import static ua.utility.kfsdbupgrade.md.base.Zips.zip;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.io.ByteSource;

import ua.utility.kfsdbupgrade.md.base.TimedInterval;

public class AnalyzeLogsTest {

  private static final Logger LOGGER = getLogger(AnalyzeLogsTest.class);

  private final String home = "/var/jenkins_home";
  private final String workspace = join(home, "workspace");
  private final String jobs = join(home, "jobs");
  private final File target = canonical(new File("./target"));
  private final String folder = "Development";
  private final File zip = getZipFile();

  private ImmutableList<String> getViews() {
    List<String> list = newArrayList();
    list.add("KFS 3 to 6 to 7 Database Upgrade - development - nonprod");
    list.add("KFS 3 to 6 to 7 Database Upgrade - nonprod");
    list.add("KFS 3 to 6 to 7 Database Upgrade on branch - nonprod");
    return newList(list);
  }

  @Test
  public void test() {
    try {
      if (parseBoolean(System.getProperty("zip"))) {
        List<String> all = getViews();
        createZip(all);
      }
      if (parseBoolean(System.getProperty("analyze"))) {
        Stopwatch sw = createStarted();
        List<String> all = getViews();
        List<View> views = processZip(all);
        Table<String, BuildKey, JobResult> table = getResults(views);
        List<DbUpgrade> upgrades = getDatabaseUpgrades(views, table);
        info(LOGGER, "database upgrade runs=%s", getCount(upgrades.size()));
        Table<String, DbUpgradeKey, DbUpgradeSummary> summary = getSummary(upgrades);
        for (String view : summary.rowKeySet()) {
          show(view, summary.row(view));
        }
        doCsv(upgrades);
        info(LOGGER, "all done -----> %s", getTime(sw));
      }
    } catch (Throwable e) {
      e.printStackTrace();
      throw illegalState(e);
    }
  }

  private String asFilename(String view) {
    String lcase = view.toLowerCase(ENGLISH);
    return lcase.replace(" ", "-").replace("---", "-").replace("--", "-");
  }

  private NotableLogEntry getEntry(List<String> lines, int index, String job, int step, LogLineType type) {
    String text = lines.get(index).replace("\n", " ").replace("\r", " ");
    NotableLogEntry.Builder builder = NotableLogEntry.builder();
    builder.withLine(index);
    builder.withJob(job);
    builder.withStep(step);
    builder.withText(text);
    builder.withType(type);
    builder.withSeverity(new SeverityProvider(type, job, text).get());
    return builder.build();
  }

  private void doCsv(List<DbUpgrade> upgrades) throws IOException {
    for (DbUpgrade upgrade : upgrades) {
      List<NotableLogEntry> list = newArrayList();
      int step = 1;
      for (JobResult result : upgrade.getResults()) {
        List<String> lines = Splitter.on('\n').splitToList(result.getOutput());
        for (int i = 0; i < lines.size(); i++) {
          String line = lines.get(i);
          LogLineType type = LogLineTypeFunction.INSTANCE.apply(line);
          if (type == LogLineType.ERROR || type == LogLineType.WARNING) {
            list.add(getEntry(lines, i, result.getJob(), step, type));
            if (line.contains("ERROR at line")) {
              list.add(list.size() - 2, getEntry(lines, i - 2, result.getJob(), step, type));
              list.add(list.size() - 1, getEntry(lines, i - 1, result.getJob(), step, type));
              list.add(getEntry(lines, i + 1, result.getJob(), step, type));
            }
          }
        }
        step++;
      }
      String funkyJenkinsToken1 = "[8mha:////4C0zH08HHAWuL5uRS+LxnHy4EdYvJVrhddF7u1IJVAhRAAAAYx+LCAAAAAAAAP9b85aBtbiIQSWjNKU4P0+vJLE4u1gvPjexLDVPzxdEhicW5WXmpfvll6S2fNly5fzGzauYGBgqihikoFqS8/OK83NS9ZwhNEghAwQwghQWAACwxA+XYgAAAA==[0m";
      String funkyJenkinsToken2 = "[8mha:////4ISWSUfg+Pzkdrw498MRMjjemKG+x8KqIS097+FNYvJwAAAAYB+LCAAAAAAAAP9b85aBtbiIQSmjNKU4P0+vJLE4u1gvPjexLDVPzxdEuhYV5Rf55ZekOlc7RKnPKH7IxMBQUcQgBdWQnJ9XnJ+TqucMoUEKGSCAEaSwAACsNFCqYAAAAA==[0m";
      String funkyJenkinsToken3 = "[8mha:////4EyDvuSZ61kzHdP1eoKfY+jRPl4tMiRJuy31musgJecDAAAAYB+LCAAAAAAAAP9b85aBtbiIQSmjNKU4P0+vJLE4u1gvPjexLDVPzxdEGvvmZ+X75ZekLlOVfvTjc8FPJgaGiiIGKaiG5Py84vycVD1nCA1SyAABjCCFBQCV27OjYAAAAA==[0m";
      Map<String, NotableLogEntry> map = newTreeMap();
      for (NotableLogEntry entry : list) {
        String key = entry.getType() + "/" + entry.getSeverity().ordinal() + "/" + leftPad(entry.getStep() + "", 3, "0") + leftPad(entry.getLine() + "", 6, "0");
        map.put(key, entry);
      }
      List<String> csv = newArrayList();
      csv.add("step,job,line,type,severity,text");
      for (NotableLogEntry entry : map.values()) {
        String text = entry.getText().replace(",", " ").replace(funkyJenkinsToken1, "").replace(funkyJenkinsToken2, "").replace(funkyJenkinsToken3, "");
        csv.add(Joiner.on(',').join(entry.getStep(), entry.getJob(), entry.getLine(), entry.getType(), entry.getSeverity(), text));
      }
      String fragment = "dbupgrades" + "/" + asFilename(upgrade.getView());
      String filename = fragment + "-" + leftPad(upgrade.getSequence() + "", 3, "0") + ".txt";
      File file = canonical(new File(target, filename));
      info(LOGGER, "creating --> %s", file);
      createParentDirs(file);
      write(Joiner.on('\n').join(csv), file, UTF_8);
    }
  }

  private void show(String view, Map<DbUpgradeKey, DbUpgradeSummary> map) {
    List<Object[]> rows = newArrayList();
    int sequence = 1;
    for (DbUpgradeSummary value : sort(map.values(), s -> s.getTiming().getStart())) {
      TimedInterval timing = value.getTiming();
      Date start = new Date(timing.getStart());
      String duration = getTime(timing.getElapsed());
      String size = getSize(value.getLogs());
      String error = getCount(value.getStats().getError());
      String warn = getCount(value.getStats().getWarn());
      String info = getCount(value.getStats().getInfo());
      String other = getCount(value.getStats().getOther());
      Object[] row = { sequence++, start, duration, value.getSteps(), size, error, warn, info, other };
      rows.add(row);
    }
    List<String> columns = asList("sequence", "start", "duration", "steps", "log output", "error", "warn", "info", "other");
    System.out.println("---------------------------------------------------------------");
    System.out.println(view);
    System.out.println("---------------------------------------------------------------");
    logTableToStdOut(columns, rows);
    System.out.println();
  }

  private ImmutableTable<String, DbUpgradeKey, DbUpgradeSummary> getSummary(Iterable<DbUpgrade> upgrades) {
    Table<String, DbUpgradeKey, DbUpgradeSummary> table = HashBasedTable.create();
    for (DbUpgrade upgrade : upgrades) {
      DbUpgradeKey key = new DbUpgradeKey(upgrade.getFolder(), upgrade.getView(), upgrade.getSequence());
      TimedInterval first = getFirst(upgrade.getResults(), null).getTiming();
      TimedInterval last = getLast(upgrade.getResults(), null).getTiming();
      int steps = upgrade.getResults().size();
      long logs = getLogOutputSize(upgrade.getResults());
      long elapsed = last.getStop() - first.getStart();
      TimedInterval timing = TimedInterval.builder().withStart(first.getStart()).withElapsed(elapsed).withStop(last.getStop()).build();
      LogStats stats = new LogStatsProvider(upgrade).get();
      DbUpgradeSummary summary = new DbUpgradeSummary(timing, steps, logs, stats);
      table.put(upgrade.getView(), key, summary);
    }
    return copyOf(table);
  }

  private long getLogOutputSize(Iterable<JobResult> results) {
    long size = 0;
    for (JobResult result : results) {
      size += result.getOutput().length();
    }
    return size;
  }

  private ImmutableList<DbUpgrade> getDatabaseUpgrades(Iterable<View> views, Table<String, BuildKey, JobResult> table) {
    List<DbUpgrade> list = newArrayList();
    for (View view : views) {
      list.addAll(getDatabaseUpgrades(view, table));
    }
    return newList(list);
  }

  private ImmutableList<DbUpgrade> getDatabaseUpgrades(View view, Table<String, BuildKey, JobResult> table) {
    List<DbUpgrade> list = newArrayList();
    Job first = view.getJobs().iterator().next();
    for (Build build : first.getBuilds()) {
      BuildKey parent = new BuildKey(folder, first.getName(), build.getNumber());
      List<JobResult> results = newArrayList();
      results.add(table.get(view.getName(), parent));
      Optional<JobResult> child = getResult(view, parent, table);
      while (child.isPresent()) {
        results.add(child.get());
        parent = new BuildKey(folder, child.get().getJob(), child.get().getBuildNumber());
        child = getResult(view, parent, table);
      }
      DbUpgrade.Builder builder = DbUpgrade.builder();
      builder.withFolder(Optional.of(folder));
      builder.withResults(results);
      builder.withSequence(list.size() + 1);
      builder.withView(view.getName());
      list.add(builder.build());
    }
    return newList(list);
  }

  private Optional<JobResult> getResult(View view, BuildKey parent, Table<String, BuildKey, JobResult> table) {
    Map<BuildKey, JobResult> map = table.row(view.getName());
    for (Entry<BuildKey, JobResult> entry : map.entrySet()) {
      JobResult result = entry.getValue();
      if (result.getParent().isPresent() && result.getParent().get().equals(parent)) {
        return of(result);
      }
    }
    return absent();
  }

  private ImmutableTable<String, BuildKey, JobResult> getResults(List<View> views) {
    JobResultFunction function = new JobResultFunction(workspace, folder);
    Table<String, BuildKey, JobResult> table = HashBasedTable.create();
    for (View view : views) {
      for (Job job : view.getJobs()) {
        for (Build build : job.getBuilds()) {
          BuildKey buildKey = new BuildKey(folder, job.getName(), build.getNumber());
          JobResult result = function.apply(build);
          table.put(view.getName(), buildKey, result);
        }
      }
    }
    return ImmutableTable.copyOf(table);
  }

  private ImmutableList<View> processZip(Iterable<String> views) throws IOException {
    Stopwatch sw = createStarted();
    info(LOGGER, "loading ------> %s [%s]", zip, getSize(zip.length()));
    ByteSource zipped = wrap(asByteSource(zip).read());
    info(LOGGER, "unzipping ----> %s", zip);
    Map<String, ByteSource> data = unzip(zipped);
    info(LOGGER, "unzipped -----> %s [%s files, %s, %s]", zip, getCount(data.size()), getSize(size(data.values())), getTime(sw));
    return new ViewsProvider(data, folder, views).get();
  }

  private long size(Iterable<ByteSource> sources) throws IOException {
    long size = 0;
    for (ByteSource source : sources) {
      size += source.size();
    }
    return size;
  }

  private void createZip(Iterable<String> views) throws IOException {
    Stopwatch sw = createStarted();
    List<File> list = newArrayList();
    File config = canonical(new File(jobs, folder + "/config.xml"));
    list.add(config);
    for (String view : views) {
      info(LOGGER, "examining ----> %s/%s", folder, view);
      List<File> filesToZip = getFilesToZip(folder, view);
      list.addAll(filesToZip);
      info(LOGGER, "located ------> %s build files", getCount(filesToZip.size()));
      info(LOGGER, "size ---------> %s", getSize(sum(filesToZip)));
    }
    info(LOGGER, "zipping ------> %s build files [%s]", getCount(list.size()), getSize(sum(list)));
    ByteSource zipped = zip(list);
    File output = canonical(new File(target, "jenkins.zip"));
    info(LOGGER, "writing ------> %s", output);
    write(zipped.read(), output);
    info(LOGGER, "all done -----> [%s]", getTime(sw));
  }

  private long sum(Iterable<File> files) {
    long sum = 0;
    for (File file : files) {
      sum += file.length();
    }
    return sum;
  }

  private ImmutableList<File> getFilesToZip(String folder, String view) throws IOException {
    File dir = new File(jobs, folder);
    JenkinsRequest request = new JenkinsRequest(dir.getPath(), view);
    List<JenkinsJob> list = getJobs(request);
    return getFilesToZip(folder, list);
  }

  protected static String join(String... strings) {
    return Joiner.on('/').join(strings);
  }

  private ImmutableList<File> getFilesToZip(String folder, Iterable<JenkinsJob> jobs) throws IOException {
    List<File> list = newArrayList();
    for (JenkinsJob job : jobs) {
      list.add(new File(job.getFolder(), "config.xml"));
      for (JenkinsBuild build : job.getBuilds()) {
        list.add(canonical(new File(build.getFolder(), "build.xml")));
        list.add(canonical(new File(build.getFolder(), "log")));
        list.add(canonical(new File(build.getFolder(), "changelog.xml")));
      }
    }
    return filter(list, isFile());
  }

  private String read(File file) throws IOException {
    return asByteSource(file).asCharSource(UTF_8).read();
  }

  private ImmutableList<JenkinsJob> getJobs(JenkinsRequest request) throws IOException {
    String config = join(request.getFolder(), "config.xml");
    String jobs = join(request.getFolder(), "jobs");
    String xml = read(new File(config));
    String view = getListView(xml, request.getView());
    List<String> jobNames = new JobNamesProvider(view).get();
    List<JenkinsJob> list = newArrayList();
    for (String jobName : jobNames) {
      File job = new File(jobs, jobName).getCanonicalFile();
      List<JenkinsBuild> builds = getBuilds(job);
      list.add(new JenkinsJob(jobName, job, builds));
    }
    return newList(list);
  }

  private String getListView(String xml, String view) {
    List<String> views = newList(substringsBetween(xml, "<hudson.model.ListView>", "</hudson.model.ListView>"));
    checkArgument(views.size() > 0, "expected to find at least one list view");
    String name = "<name>" + view + "</name>";
    List<String> filtered = filter(views, s -> s.contains(name));
    checkArgument(filtered.size() == 1, "expected to find exactly 1 list view matching %s", name);
    return filtered.iterator().next();
  }

  private ImmutableList<JenkinsBuild> getBuilds(File job) {
    checkArgument(job.isDirectory(), "%s is not a directory", job);
    File builds = new File(job, "builds");
    checkArgument(builds.isDirectory(), "%s is not a directory", builds);
    List<JenkinsBuild> list = newArrayList();
    for (File build : builds.listFiles()) {
      if (build.isDirectory()) {
        try {
          int buildNumber = parseInt(build.getName());
          list.add(new JenkinsBuild(buildNumber, build));
        } catch (NumberFormatException e) {
          // ignore
        }
      }
    }
    return newList(list);
  }

  private File getZipFile() {
    String zipFile = System.getProperty("zip.file");
    if (zipFile == null) {
      return getDefaultZipFile();
    } else {
      return canonical(new File(zipFile));
    }
  }

  private File getDefaultZipFile() {
    String home = USER_HOME.value();
    return canonical(new File(home, "Downloads/jenkins.zip"));
  }

}
