package ua.utility.kfsdbupgrade.mdoc.simple;

import static com.google.common.collect.Lists.newArrayList;
import static ua.utility.kfsdbupgrade.mdoc.Closeables.closeQuietly;
import static ua.utility.kfsdbupgrade.mdoc.Formats.getCount;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.log.Logging;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;

public final class RowIdProvider implements Provider<ImmutableList<RowId>> {

  private static final Logger LOGGER = Logger.getLogger(RowIdProvider.class);

  public RowIdProvider(Connection conn, int max) {
    this.conn = conn;
    this.max = max;
  }

  private final Connection conn;
  private final int max;
  private final RowIdConverter converter = RowIdConverter.getInstance();

  @Override
  public ImmutableList<RowId> get() {
    Statement stmt = null;
    ResultSet rs = null;
    List<RowId> list = newArrayList();
    try {
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT ROWID FROM KRNS_MAINT_DOC_T");
      int count = 0;
      while (rs.next()) {
        String rowId = rs.getString(1);
        RowId element = converter.convert(rowId);
        list.add(element);
        count++;
        if (count % 50000 == 0) {
          Logging.info(LOGGER, "%s", getCount(count));
        }
        if (count == max) {
          break;
        }
      }
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
    return newList(list);
  }

}
