package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Long.parseLong;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ua.utility.kfsdbupgrade.analysis.JobResultType.UNKNOWN;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import ua.utility.kfsdbupgrade.md.base.TimedInterval;
import ua.utility.kfsdbupgrade.md.base.Xml;

public final class JobResultFunction implements Function<Build, JobResult> {

  public JobResultFunction(String workspace, String folder) {
    this(workspace, of(folder));
  }

  public JobResultFunction(String workspace, Optional<String> folder) {
    checkArgument(isNotBlank(workspace), "workspace cannot be blank");
    checkArgument(folder.isPresent() ? isNotBlank(folder.get()) : true, "folder cannot be blank");
    this.workspace = workspace;
    this.folder = folder;
  }

  private final String workspace;
  private final Optional<String> folder;

  public JobResult apply(Build input) {
    // parse the build output for important values
    String workspace = substringBetween(input.getBuildXml(), "<workspace>", "</workspace>");
    String fragment = removeStart(workspace, this.workspace);
    String job = Xml.UnescapeFunction.INSTANCE.apply(removeStart(folder.isPresent() ? removeStart(fragment, "/" + folder.get()) : fragment, "/"));
    JobResultType result = UNKNOWN;
    String resultString = substringBetween(input.getBuildXml(), "<result>", "</result>");
    if (resultString != null) {
      result = JobResultType.valueOf(resultString);
    }
    long start = parseLong(substringBetween(input.getBuildXml(), "<startTime>", "</startTime>"));
    long elapsed = parseLong(substringBetween(input.getBuildXml(), "<duration>", "</duration>"));
    TimedInterval timing = TimedInterval.build(start, elapsed);
    Optional<BuildKey> parent = absent();
    if (input.getBuildXml().contains("<upstreamProject>")) {
      parent = of(new ParentProvider(input.getBuildXml()).get());
    }

    // store everything in an immutable object
    JobResult.Builder builder = JobResult.builder();
    builder.withParent(parent);
    builder.withJob(job);
    builder.withBuildNumber(input.getNumber());
    builder.withResult(result);
    builder.withOutput(input.getLog());
    builder.withTiming(timing);
    builder.withFolder(folder);
    return builder.build();
  }

}
