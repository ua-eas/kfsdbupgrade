package ua.utility.kfsdbupgrade;

import static ua.utility.kfsdbupgrade.mdoc.simple.Exceptions.illegalState;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.base.Function;

import ua.utility.kfsdbupgrade.mdoc.MaintDoc;

public enum MaintDocFunction implements Function<ResultSet, MaintDoc> {
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