package ua.utility.kfsdbupgrade.rds;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static ua.utility.kfsdbupgrade.md.base.Lists.filter;
import static ua.utility.kfsdbupgrade.md.base.Splitters.csv;
import static ua.utility.kfsdbupgrade.md.base.Splitters.split;
import static ua.utility.kfsdbupgrade.rds.Rds.DEFAULT_AWS_ACCOUNT;
import static ua.utility.kfsdbupgrade.rds.Rds.DEFAULT_ENVIRONMENT;
import static ua.utility.kfsdbupgrade.rds.Rds.checkedName;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public final class TagsProvider implements Provider<ImmutableMap<String, Optional<String>>> {

  public TagsProvider(String name, Properties props) {
    this.name = checkedName(name);
    this.props = checkNotNull(props);
  }

  private final String name;
  private final Properties props;

  public ImmutableMap<String, Optional<String>> get() {
    return getTags(getDefaultTags(props, name));
  }

  private ImmutableMap<String, Optional<String>> getTags(Map<String, Optional<String>> defaultTags) {
    Map<String, Optional<String>> map = newLinkedHashMap();
    map.putAll(defaultTags);
    for (String tag : csv(props.getProperty("rds.tags", ""))) {
      Iterator<String> itr = split('=', tag).iterator();
      String key = itr.next();
      Optional<String> value = itr.hasNext() ? Optional.of(itr.next()) : Optional.<String>absent();
      map.put(key, value);
    }
    return ImmutableMap.copyOf(map);
  }

  private ImmutableMap<String, Optional<String>> getDefaultTags(Properties props, String name) {
    String account = props.getProperty("rds.account", DEFAULT_AWS_ACCOUNT);
    Map<String, Optional<String>> map = newLinkedHashMap();
    map.put("service", fromNullable(props.getProperty("rds.tag.service", account)));
    map.put("accountnumber", fromNullable(props.getProperty("rds.tag.accountnumber", account)));
    map.put("subaccount", fromNullable(props.getProperty("rds.tag.subaccount", account)));
    map.put("name", fromNullable(props.getProperty("rds.tag.name", name)));
    map.put("environment", fromNullable(props.getProperty("rds.tag.environment", DEFAULT_ENVIRONMENT)));
    String prefix = "rds.tag.";
    for (String key : filter(props.stringPropertyNames(), key -> key.startsWith(prefix))) {
      map.put(removeStart(key, prefix), fromNullable(props.getProperty(key)));
    }
    return ImmutableMap.copyOf(map);
  }

}
