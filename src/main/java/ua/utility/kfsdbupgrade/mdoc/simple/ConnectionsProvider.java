package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static ua.utility.kfsdbupgrade.log.Logging.info;

import java.sql.Connection;
import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

public final class ConnectionsProvider implements Provider<ImmutableList<Connection>> {

  private static final Logger LOGGER = Logger.getLogger(ConnectionsProvider.class);

  public ConnectionsProvider(Provider<Connection> provider, int count, Connection existing) {
    this(provider, count, asList(existing));
  }

  public ConnectionsProvider(Provider<Connection> provider, int count, Iterable<Connection> existing) {
    checkArgument(count > 0, "count must be greater than zero");
    this.provider = checkNotNull(provider);
    this.count = count;
    this.existing = copyOf(existing);
  }

  private final Provider<Connection> provider;
  private final int count;
  private final ImmutableList<Connection> existing;

  public ImmutableList<Connection> get() {
    info(LOGGER, "establishing %s connections (%s existing, %s new)", count, existing.size(), count - existing.size());
    List<Connection> conns = newArrayList();
    conns.addAll(existing);
    for (int i = existing.size(); i < count; i++) {
      conns.add(provider.get());
    }
    return copyOf(conns);
  }

}
