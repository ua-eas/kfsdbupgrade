package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

public final class StringProvider implements Provider<ImmutableList<String>> {

  private static final Logger LOGGER = getLogger(StringProvider.class);

  public StringProvider(Provider<Connection> provider) {
    this(provider, Optional.<Integer>absent());
  }

  public StringProvider(Provider<Connection> provider, int max) {
    this(provider, of(max), "ROWID");
  }

  public StringProvider(Provider<Connection> provider, int max, String field) {
    this(provider, of(max), field);
  }

  public StringProvider(Provider<Connection> provider, Optional<Integer> max) {
    this(provider, max, "ROWID");
  }

  public StringProvider(Provider<Connection> provider, Optional<Integer> max, String field) {
    checkArgument(max.isPresent() ? max.get() > 0 : true, "max must be greater than zero");
    this.provider = checkNotNull(provider);
    this.max = max;
    this.field = field.toUpperCase();
  }

  private final Provider<Connection> provider;
  private final Optional<Integer> max;
  private final String field;

  public ImmutableList<String> get() {
    List<String> list = newArrayList();
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      Stopwatch sw = createStarted();
      info(LOGGER, "field:%s count:%s", field, max.isPresent() ? max.get() : "all");
      conn = provider.get();
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT %s FROM KRNS_MAINT_DOC_T");
      while (rs.next()) {
        list.add(rs.getString(1));
        if (max.isPresent() && list.size() >= max.get()) {
          break;
        }
      }
      info(LOGGER, "field:%s count:%s [%s]", field, max.isPresent() ? max.get() : "all", getTime(sw));
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }
    return copyOf(list);
  }

}
