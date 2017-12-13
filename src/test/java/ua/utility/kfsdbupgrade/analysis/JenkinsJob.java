package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;

import java.io.File;

import com.google.common.collect.ImmutableList;

public final class JenkinsJob {

  public JenkinsJob(String name, File folder, Iterable<JenkinsBuild> builds) {
    this.name = checkNotNull(name);
    this.folder = checkNotNull(folder);
    this.builds = newList(builds);
  }

  private final String name;
  private final File folder;
  private final ImmutableList<JenkinsBuild> builds;

  public String getName() {
    return name;
  }

  public File getFolder() {
    return folder;
  }

  public ImmutableList<JenkinsBuild> getBuilds() {
    return builds;
  }

}
