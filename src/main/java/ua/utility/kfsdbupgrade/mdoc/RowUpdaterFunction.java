package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;

public class RowUpdaterFunction implements Function<SelectContext<MaintDoc>, RowUpdater<MaintDoc>> {

  public RowUpdaterFunction(DatabaseMetrics metrics) {
    this.metrics = checkNotNull(metrics);
  }

  private final DatabaseMetrics metrics;

  public RowUpdater<MaintDoc> apply(SelectContext<MaintDoc> input) {
    RowSelector<MaintDoc> selector = input.getSelector();
    RowUpdater.Builder<MaintDoc> builder = RowUpdater.builder();
    // builder.withProvider(selector.getProvider());
    builder.withTable(selector.getTable());
    // builder.withWeigher(selector.getWeigher());
    builder.withField("DOC_CNTNT");
    builder.withBatch(DocBatchFunction.INSTANCE);
    builder.withWhere("ROWID");
    builder.withEntitities(input.getSelected());
    builder.withCloseConnection(false);
    builder.withMetrics(metrics);
    return builder.build();
  }

}