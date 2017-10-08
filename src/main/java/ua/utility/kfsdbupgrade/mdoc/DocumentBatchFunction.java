package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Stopwatch.createStarted;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ua.utility.kfsdbupgrade.mdoc.Exceptions.illegalState;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;

public final class DocumentBatchFunction implements Function<MaintDoc, Long> {

  public DocumentBatchFunction(PreparedStatement pstmt) {
    this.pstmt = checkNotNull(pstmt);
  }

  private final PreparedStatement pstmt;

  @Override
  public Long apply(MaintDoc input) {
    Stopwatch sw = createStarted();
    try {
      pstmt.setString(1, input.getContent());
      pstmt.setString(2, input.getId());
      pstmt.addBatch();
    } catch (SQLException e) {
      throw illegalState(e);
    }
    return sw.elapsed(MILLISECONDS);
  }

}
