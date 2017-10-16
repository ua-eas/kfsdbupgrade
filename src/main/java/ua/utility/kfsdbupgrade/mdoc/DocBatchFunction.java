package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Stopwatch.createStarted;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;

public enum DocBatchFunction implements Function<BatchContext<MaintDoc>, Long> {
  INSTANCE;

  @Override
  public Long apply(BatchContext<MaintDoc> input) {
    Stopwatch sw = createStarted();
    try {
      PreparedStatement pstmt = input.getPstmt();
      MaintDoc doc = input.getInstance();
      pstmt.setString(1, doc.getContent());
      pstmt.setString(2, doc.getId());
      pstmt.addBatch();
    } catch (SQLException e) {
      throw illegalState(e);
    }
    return sw.elapsed(MILLISECONDS);
  }

}
