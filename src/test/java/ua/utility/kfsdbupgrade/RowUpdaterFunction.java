package ua.utility.kfsdbupgrade;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;

import ua.utility.kfsdbupgrade.mdoc.DataMetrics;
import ua.utility.kfsdbupgrade.mdoc.DocBatchFunction;
import ua.utility.kfsdbupgrade.mdoc.MaintDoc;
import ua.utility.kfsdbupgrade.mdoc.RowSelector;
import ua.utility.kfsdbupgrade.mdoc.RowUpdater;
import ua.utility.kfsdbupgrade.mdoc.SelectContext;

public class RowUpdaterFunction implements Function<SelectContext<MaintDoc>, RowUpdater<MaintDoc>> {

  public RowUpdaterFunction(int show, DataMetrics overall, DataMetrics current, Stopwatch timer, Stopwatch last) {
    this.show = show;
    this.overall = checkNotNull(overall);
    this.current = checkNotNull(current);
    this.timer = checkNotNull(timer);
    this.last = checkNotNull(last);
  }

  private final int show;
  private final DataMetrics overall;
  private final DataMetrics current;
  private final Stopwatch timer;
  private final Stopwatch last;

  public RowUpdater<MaintDoc> apply(SelectContext<MaintDoc> input) {
    RowSelector<MaintDoc> selector = input.getSelector();
    RowUpdater.Builder<MaintDoc> builder = RowUpdater.builder();
    builder.withProvider(selector.getProvider());
    builder.withTable(selector.getTable());
    builder.withWeigher(selector.getWeigher());
    builder.withField("DOC_CNTNT");
    builder.withBatch(DocBatchFunction.INSTANCE);
    builder.withWhere("ROWID");
    builder.withEntitities(input.getSelected());
    builder.withShow(show);
    builder.withOverall(overall);
    builder.withCurrent(current);
    builder.withTimer(timer);
    builder.withLast(last);
    builder.withCloseConnection(false);
    builder.withShowFinal(false);
    return builder.build();
  }

  public int getShow() {
    return show;
  }

  public DataMetrics getOverall() {
    return overall;
  }

  public DataMetrics getCurrent() {
    return current;
  }

  public Stopwatch getTimer() {
    return timer;
  }

  public Stopwatch getLast() {
    return last;
  }
}