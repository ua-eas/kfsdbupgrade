package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public final class DbUpgrade {

  private final Optional<String> folder;
  private final String view;
  private final int sequence;
  private final ImmutableList<JobResult> results;

  private DbUpgrade(Builder builder) {
    this.folder = builder.folder;
    this.view = builder.view;
    this.sequence = builder.sequence;
    this.results = newList(builder.results);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Optional<String> folder = absent();
    private String view;
    private int sequence;
    private List<JobResult> results = newArrayList();

    public Builder withFolder(Optional<String> folder) {
      this.folder = folder;
      return this;
    }

    public Builder withView(String view) {
      this.view = view;
      return this;
    }

    public Builder withSequence(int sequence) {
      this.sequence = sequence;
      return this;
    }

    public Builder withResults(List<JobResult> results) {
      this.results = results;
      return this;
    }

    public DbUpgrade build() {
      return validate(new DbUpgrade(this));
    }

    private static DbUpgrade validate(DbUpgrade instance) {
      checkArgument(instance.folder.isPresent() ? isNotBlank(instance.folder.get()) : true, "folder cannot be blank");
      checkArgument(isNotBlank(instance.view), "view cannot be blank");
      checkArgument(instance.sequence >= 1, "sequence must be >= 1");
      return instance;
    }
  }

  public Optional<String> getFolder() {
    return folder;
  }

  public String getView() {
    return view;
  }

  public int getSequence() {
    return sequence;
  }

  public ImmutableList<JobResult> getResults() {
    return results;
  }

}
