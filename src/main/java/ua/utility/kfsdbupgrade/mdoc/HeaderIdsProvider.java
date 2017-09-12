package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
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

public class HeaderIdsProvider implements Provider<ImmutableList<String>> {

  private static final Logger LOGGER = getLogger(HeaderIdsProvider.class);

  public HeaderIdsProvider(Provider<Connection> provider, Optional<Integer> max) {
    checkArgument(max.isPresent() ? max.get() > 0 : true, "max must be greater than zero");
    this.provider = checkNotNull(provider);
    this.max = max;
  }

  private final Provider<Connection> provider;
  private final Optional<Integer> max;

  public ImmutableList<String> get() {
    List<String> headerIds = newArrayList();
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      Stopwatch sw = createStarted();
      info("acquiring %s document header ids", max.isPresent() ? max.get() : "all");
      conn = provider.get();
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT DOC_HDR_ID FROM KRNS_MAINT_DOC_T");
      int count = 0;
      while (rs.next()) {
        headerIds.add(rs.getString(1));
        count++;
        if (max.isPresent() && count >= max.get()) {
          break;
        }
      }
      info("acquired %s document header ids [%s]", getCount(headerIds.size()), getTime(sw));
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }
    return copyOf(headerIds);
  }

  private void info(String msg, Object... args) {
    if (args == null || args.length == 0) {
      LOGGER.info(msg);
    } else {
      LOGGER.info(format(msg, args));
    }
  }

}
