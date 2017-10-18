package ua.utility.kfsdbupgrade.md;

import static com.google.common.collect.Lists.newArrayList;
import static ua.utility.kfsdbupgrade.md.base.Lists.transform;

import java.util.List;

import com.google.common.base.Joiner;

public final class Jdbc {

  private Jdbc() {
  }

  private static final RowIdConverter ROW_ID_CONVERTER = RowIdConverter.getInstance();

  public static <T> String asInClause(Iterable<RowId> rowIds) {
    return asInClause(transform(rowIds, ROW_ID_CONVERTER.reverse()), true);
  }

  public static <T> String asInClause(Iterable<T> iterable, boolean quote) {
    List<String> list = newArrayList();
    for (T element : iterable) {
      if (quote) {
        list.add("'" + element.toString() + "'");
      } else {
        list.add(element.toString());
      }
    }
    return Joiner.on(',').join(list);
  }

}
