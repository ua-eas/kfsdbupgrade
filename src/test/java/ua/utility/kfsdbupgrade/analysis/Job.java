package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ua.utility.kfsdbupgrade.md.base.Lists.sort;

import com.google.common.collect.ImmutableList;

public final class Job {

  public Job(String name, String config, Iterable<Build> builds) {
    checkArgument(isNotBlank(name), "name cannot be blank");
    checkArgument(isNotBlank(config), "config cannot be blank");
    this.name = name;
    this.config = config;
    this.builds = sort(builds, b -> b.getNumber());
  }

  private final String name;
  private final String config;
  private final ImmutableList<Build> builds;

  public String getName() {
    return name;
  }

  public String getConfig() {
    return config;
  }

  public ImmutableList<Build> getBuilds() {
    return builds;
  }

}
