package ua.utility.kfsdbupgrade.md;

import static ua.utility.kfsdbupgrade.md.base.Exceptions.illegalState;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.base.Function;

public enum SingleIntegerFunction implements Function<ResultSet, Integer> {
  INSTANCE;

  public Integer apply(ResultSet input) {
    try {
      return input.getInt(1);
    } catch (SQLException e) {
      throw illegalState(e);
    }
  }

}
