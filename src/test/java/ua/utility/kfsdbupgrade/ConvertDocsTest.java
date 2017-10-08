package ua.utility.kfsdbupgrade;

import static com.google.common.base.Stopwatch.createUnstarted;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static ua.utility.kfsdbupgrade.log.Logging.info;
import static ua.utility.kfsdbupgrade.mdoc.Exceptions.illegalState;
import static ua.utility.kfsdbupgrade.mdoc.Lists.distribute;
import static ua.utility.kfsdbupgrade.mdoc.Lists.newList;
import static ua.utility.kfsdbupgrade.mdoc.Lists.shuffle;
import static ua.utility.kfsdbupgrade.mdoc.Lists.transform;
import static ua.utility.kfsdbupgrade.mdoc.Providers.of;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import ua.utility.kfsdbupgrade.mdoc.ConnectionProvider;
import ua.utility.kfsdbupgrade.mdoc.DataMetrics;
import ua.utility.kfsdbupgrade.mdoc.DocBatchFunction;
import ua.utility.kfsdbupgrade.mdoc.MaintDoc;
import ua.utility.kfsdbupgrade.mdoc.PropertiesProvider;
import ua.utility.kfsdbupgrade.mdoc.RowId;
import ua.utility.kfsdbupgrade.mdoc.RowIdConverter;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.RowUpdater;
import ua.utility.kfsdbupgrade.mdoc.SingleStringFunction;
import ua.utility.kfsdbupgrade.mdoc.StringWeigher;
import ua.utility.kfsdbupgrade.mdoc.ThreadsProvider;

public class ConvertDocsTest {

  private static final Logger LOGGER = Logger.getLogger(ConvertDocsTest.class);

  private final RowIdConverter converter = RowIdConverter.getInstance();

  @Test
  public void test() {
    try {
      System.setProperty("mdoc.threads", "2");
      Properties props = new PropertiesProvider().get();
      String table = "KRNS_MAINT_DOC_T";
      List<RowId> ids = getRowIds(props, table, 100, 33);
      List<RowSelector<MaintDoc>> selectors = getSelectors(props, ids, 33);
      for (RowSelector<MaintDoc> selector : selectors) {
        info(LOGGER, "-- select --");
        List<MaintDoc> selected = selector.get();
        RowUpdater<MaintDoc> updater = getUpdater(selector, selected, 10);
        info(LOGGER, "-- update --");
        List<MaintDoc> updated = updater.get();
      }
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private static RowUpdater<MaintDoc> getUpdater(RowSelector<MaintDoc> selector, List<MaintDoc> docs, int show) {
    RowUpdater.Builder<MaintDoc> builder = RowUpdater.builder();
    builder.withProvider(selector.getProvider());
    builder.withTable(selector.getTable());
    builder.withField("DOC_CNTNT");
    builder.withWeigher(selector.getWeigher());
    builder.withBatch(DocBatchFunction.INSTANCE);
    builder.withWhere("ROWID");
    builder.withEntitities(docs);
    builder.withShow(show);
    builder.withBatchSize(3);
    return builder.build();
  }

  // setup selectors that extract maintenance documents
  private static ImmutableList<RowSelector<MaintDoc>> getSelectors(Properties props, List<RowId> ids, int show) {
    RowIdConverter converter = RowIdConverter.getInstance();
    List<String> rowIds = shuffle(transform(ids, converter.reverse()));
    int threads = new ThreadsProvider(props).get();
    DataMetrics overall = new DataMetrics();
    DataMetrics current = new DataMetrics();
    Stopwatch timer = createUnstarted();
    Stopwatch last = createUnstarted();
    List<RowSelector<MaintDoc>> list = newArrayList();
    for (List<String> distribution : distribute(rowIds, threads)) {
      Provider<Connection> provider = of(new ConnectionProvider(props, false).get());
      RowSelector.Builder<MaintDoc> builder = RowSelector.builder();
      builder.withFunction(MaintDocFunction.INSTANCE);
      builder.withWeigher(MaintDocWeigher.INSTANCE);
      builder.withRowIds(distribution);
      builder.withShow(show);
      builder.withTable("KRNS_MAINT_DOC_T");
      builder.withProvider(provider);
      builder.withFields(asList("ROWID", "DOC_CNTNT"));
      builder.withOverall(overall);
      builder.withCurrent(current);
      builder.withTimer(timer);
      builder.withLast(last);
      builder.withCloseConnection(false);
      RowSelector<MaintDoc> selector = builder.build();
      list.add(selector);
    }
    return newList(list);
  }

  private enum MaintDocFunction implements Function<ResultSet, MaintDoc> {
    INSTANCE;

    public MaintDoc apply(ResultSet rs) {
      try {
        String id = rs.getString(1);
        String content = rs.getString(2);
        return MaintDoc.build(id, content);
      } catch (SQLException e) {
        throw illegalState(e);
      }
    }
  }

  private enum MaintDocWeigher implements Function<MaintDoc, Long> {
    INSTANCE;

    public Long apply(MaintDoc input) {
      return 0L + input.getId().length() + input.getContent().length();
    }
  }

  private ImmutableList<RowId> getRowIds(Properties props, String table, int max, int show) {
    ConnectionProvider provider = new ConnectionProvider(props, false);
    RowSelector.Builder<String> builder = RowSelector.builder();
    builder.withFunction(SingleStringFunction.INSTANCE);
    builder.withWeigher(StringWeigher.INSTANCE);
    builder.withMax(max);
    builder.withShow(show);
    builder.withTable(table);
    builder.withProvider(provider);
    RowSelector<String> selector = builder.build();
    List<String> strings = selector.get();
    return transform(strings, converter);
  }

}
