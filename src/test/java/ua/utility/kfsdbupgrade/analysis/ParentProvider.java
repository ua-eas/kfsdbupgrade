package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBetween;

import java.util.Iterator;

import javax.inject.Provider;

import com.google.common.base.Splitter;

import ua.utility.kfsdbupgrade.md.base.Xml;

public final class ParentProvider implements Provider<BuildKey> {

  public ParentProvider(String buildXml) {
    checkArgument(isNotBlank(buildXml), "buildXml cannot be blank");
    this.buildXml = buildXml;
  }

  private final String buildXml;

  public BuildKey get() {
    String causeAction = substringBetween(buildXml, "<hudson.model.CauseAction>", "</hudson.model.CauseAction>");
    String upstreamProject = Xml.UnescapeFunction.INSTANCE.apply(substringBetween(causeAction, "<upstreamProject>", "</upstreamProject>"));
    String upstreamBuild = substringBetween(causeAction, "<upstreamBuild>", "</upstreamBuild>");
    if (upstreamProject.contains("/")) {
      Iterator<String> itr = Splitter.on('/').split(upstreamProject).iterator();
      String folder = itr.next();
      String job = itr.next();
      return new BuildKey(folder, job, parseInt(upstreamBuild));
    } else {
      return new BuildKey(upstreamProject, parseInt(upstreamBuild));
    }
  }

}
