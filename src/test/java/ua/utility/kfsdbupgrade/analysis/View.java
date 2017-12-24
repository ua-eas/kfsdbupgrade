package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.collect.ImmutableList.copyOf;

import com.google.common.collect.ImmutableList;

public final class View {

  public View(String name, Iterable<Job> jobs) {
    this.name = name;
    this.jobs = copyOf(jobs);
  }

  private final String name;
  private final ImmutableList<Job> jobs;

  public String getName() {
    return name;
  }

  public ImmutableList<Job> getJobs() {
    return jobs;
  }

}