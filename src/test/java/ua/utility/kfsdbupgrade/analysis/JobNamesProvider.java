package ua.utility.kfsdbupgrade.analysis;

import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.substringsBetween;
import static ua.utility.kfsdbupgrade.md.base.Lists.newList;
import static ua.utility.kfsdbupgrade.md.base.Lists.transform;

import java.util.List;

import javax.inject.Provider;

import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.md.base.Xml;

public final class JobNamesProvider implements Provider<ImmutableList<String>> {

  public JobNamesProvider(String listViewXml) {
    this.listViewXml = listViewXml;
  }

  private final String listViewXml;

  public ImmutableList<String> get() {
    String jobNames = substringBetween(listViewXml, "<jobNames>", "</jobNames>");
    List<String> raw = newList(substringsBetween(jobNames, "<string>", "</string>"));
    return transform(raw, Xml.UnescapeFunction.INSTANCE);
  }

}
