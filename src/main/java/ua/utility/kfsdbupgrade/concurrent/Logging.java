package ua.utility.kfsdbupgrade.concurrent;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.max;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public final class Logging {

  public static void logTableToStdOut(List<String> columns, List<Object[]> rows) {
    List<Column> cols = getColumns(columns, rows);
    List<String> tokens = newArrayList();
    for (int i = 0; i < cols.size(); i++) {
      Column col = cols.get(i);
      tokens.add(rightPad(col.getName(), col.getWidth()));
    }
    System.out.println(Joiner.on(' ').join(tokens));
    for (int i = 0; i < rows.size(); i++) {
      Object[] row = rows.get(i);
      tokens = newArrayList();
      for (int j = 0; j < cols.size(); j++) {
        Column col = cols.get(j);
        tokens.add(rightPad(row[j].toString(), col.getWidth()));
      }
      System.out.println(Joiner.on(' ').join(tokens));
    }

  }

  private static List<Column> getColumns(List<String> columns, List<Object[]> table) {
    int[] widths = new int[columns.size()];
    for (int i = 0; i < table.size(); i++) {
      Object[] row = table.get(i);
      for (int j = 0; j < columns.size(); j++) {
        widths[j] = max(widths[j], row[j].toString().length());
      }
    }
    for (int i = 0; i < columns.size(); i++) {
      widths[i] = max(widths[i], columns.get(i).length());
    }
    List<Column> list = newArrayList();
    for (int i = 0; i < columns.size(); i++) {
      list.add(new Column(columns.get(i), widths[i]));
    }
    return ImmutableList.copyOf(list);
  }

  private static class Column {

    public Column(String name, int width) {
      this.width = width;
      this.name = name;
    }

    private final String name;
    private final int width;

    public String getName() {
      return name;
    }

    public int getWidth() {
      return width;
    }

    @Override
    public String toString() {
      return name;
    }
  }

}
