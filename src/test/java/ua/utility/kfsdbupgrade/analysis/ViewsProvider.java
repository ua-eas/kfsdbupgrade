package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.substringsBetween;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.analysis.AnalyzeLogsTest.join;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;
import static ua.utility.kfsdbupgrade.md.base.Lists.filter;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;
import static ua.utility.kfsdbupgrade.md.base.Lists.sort;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;

public final class ViewsProvider implements Provider<ImmutableList<View>> {

  private static final Logger LOGGER = getLogger(ViewsProvider.class);

  private final String home = "/var/jenkins_home";
  private final String jobs = join(home, "jobs");

  public ViewsProvider(Map<String, ByteSource> data, String folder, Iterable<String> views) {
    this(data, of(folder), views);
  }

  public ViewsProvider(Map<String, ByteSource> data, Optional<String> folder, Iterable<String> views) {
    checkArgument(folder.isPresent() ? isNotBlank(folder.get()) : true, "folder cannot be blank");
    this.data = copyOf(data);
    this.folder = folder;
    this.views = copyOf(views);
  }

  private final ImmutableMap<String, ByteSource> data;
  private final Optional<String> folder;
  private final ImmutableList<String> views;

  public ImmutableList<View> get() {
    try {
      String configXml = getConfigXml(data, folder);
      List<View> list = newArrayList();
      for (String view : views) {
        info(LOGGER, "processing -> %s", view);
        String listViewXml = getListViewXml(configXml, view);
        List<String> jobNames = new JobNamesProvider(listViewXml).get();
        List<Job> jobs = newArrayList();
        for (String jobName : jobNames) {
          String key = this.jobs + (folder.isPresent() ? "/" + folder.get() + "/jobs" : "") + "/" + jobName + "/config.xml";
          String config = data.get(key).asCharSource(UTF_8).read();
          List<Build> builds = getBuilds(data, folder, jobName);
          jobs.add(new Job(jobName, config, builds));
        }
        list.add(new View(view, jobs));
      }
      return newList(list);
    } catch (IOException e) {
      throw illegalState(e);
    }
  }

  private ImmutableList<Build> getBuilds(Map<String, ByteSource> data, Optional<String> folder, String job) throws IOException {
    String prefix = jobs + (folder.isPresent() ? "/" + folder.get() + "/jobs" : "") + "/" + job + "/builds";
    List<Integer> buildNumbers = getBuildNumbers(data, prefix);
    List<Build> list = newArrayList();
    for (Integer buildNumber : buildNumbers) {
      String buildXmlKey = prefix + "/" + buildNumber + "/build.xml";
      if (data.containsKey(buildXmlKey)) {
        String buildXml = fixed(asPrintableAsciiWithLineFeeds(data.get(buildXmlKey)));
        String log = fixed(asPrintableAsciiWithLineFeeds(data.get(prefix + "/" + buildNumber + "/log")));
        String changeLogXmlKey = prefix + "/" + buildNumber + "/changelog.xml";
        if (data.containsKey(changeLogXmlKey)) {
          Optional<String> changeLogXml = fromNullable(trimToNull(data.get(changeLogXmlKey).asCharSource(UTF_8).read()));
          list.add(Build.builder().withNumber(buildNumber).withBuildXml(buildXml).withLog(log).withChangeLogXml(changeLogXml).build());
        } else {
          list.add(Build.builder().withNumber(buildNumber).withBuildXml(buildXml).withLog(log).build());
        }
      }
    }
    return newList(list);
  }

  private String fixed(String input) {
    return input.replace(
        "[8mha:////4C0zH08HHAWuL5uRS+LxnHy4EdYvJVrhddF7u1IJVAhRAAAAYx+LCAAAAAAAAP9b85aBtbiIQSWjNKU4P0+vJLE4u1gvPjexLDVPzxdEhicW5WXmpfvll6S2fNly5fzGzauYGBgqihikoFqS8/OK83NS9ZwhNEghAwQwghQWAACwxA+XYgAAAA==[0m",
        "");
  }

  private String asPrintableAsciiWithLineFeeds(ByteSource bytes) throws IOException {
    return getAscii(bytes.asCharSource(UTF_8).read());
  }

  private boolean isPrintable(char c) {
    return c >= 32 && c <= 126;
  }

  private String getAscii(String s) {
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
      if (isPrintable(c) || c == '\n') {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private ImmutableList<Integer> getBuildNumbers(Map<String, ByteSource> data, String prefix) {
    Set<Integer> builds = newHashSet();
    for (String key : filter(data.keySet(), s -> s.startsWith(prefix))) {
      String fragment = removeStart(key, prefix);
      String token = substringBetween(fragment, "/", "/");
      int buildNumber = parseInt(token);
      builds.add(buildNumber);
    }
    return sort(builds);
  }

  private String getConfigXml(Map<String, ByteSource> data, Optional<String> folder) throws IOException {
    String key = jobs + (folder.isPresent() ? "/" + folder.get() : "") + "/config.xml";
    return data.get(key).asCharSource(UTF_8).read();
  }

  private String getListViewXml(String configXml, String view) {
    List<String> views = newList(substringsBetween(configXml, "<hudson.model.ListView>", "</hudson.model.ListView>"));
    checkArgument(views.size() > 0, "expected to find at least one list view");
    String name = "<name>" + view + "</name>";
    List<String> filtered = filter(views, s -> s.contains(name));
    checkArgument(filtered.size() == 1, "expected to find exactly 1 list view matching %s", name);
    return filtered.iterator().next();
  }

}
