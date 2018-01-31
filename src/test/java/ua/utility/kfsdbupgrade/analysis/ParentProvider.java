package ua.utility.kfsdbupgrade.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.substringsBetween;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;

import java.util.Iterator;
import java.util.List;

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
        List<String> causeActions = newList(substringsBetween(buildXml, "<hudson.model.CauseAction>", "</hudson.model.CauseAction>"));
        String causeAction = getCauseAction(causeActions);
        String upstreamFragment = substringBetween(causeAction, "<upstreamProject>", "</upstreamProject>");
        if (upstreamFragment == null) {
            System.out.println("\n" + buildXml + "\n");
        }
        String upstreamProject = Xml.UnescapeFunction.INSTANCE.apply(upstreamFragment);
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

    private String getCauseAction(Iterable<String> causeActions) {
        for (String causeAction : causeActions) {
            if (causeAction.contains("<upstreamProject>")) {
                return causeAction;
            }
        }
        throw illegalState("could not locate upstream project");
    }

}
