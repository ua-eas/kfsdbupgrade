package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.PreparedStatement;

public final class BatchContext<T> {

  public BatchContext(T instance, PreparedStatement pstmt) {
    this.instance = checkNotNull(instance);
    this.pstmt = checkNotNull(pstmt);
  }

  private final T instance;
  private final PreparedStatement pstmt;

  public T getInstance() {
    return instance;
  }

  public PreparedStatement getPstmt() {
    return pstmt;
  }

}
