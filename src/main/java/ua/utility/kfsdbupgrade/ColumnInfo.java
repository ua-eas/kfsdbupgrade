package ua.utility.kfsdbupgrade;

public class ColumnInfo implements Comparable<ColumnInfo>  {
	private String columnName;
	private int seq;
	private boolean numeric = false;
	
	public ColumnInfo(String columnName, int seq) {
		this.columnName = columnName;
		this.seq = seq;
	}
	
	public String getColumnName() {
		return columnName;
	}
	
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	public int getSeq() {
		return seq;
	}
	
	public void setSeq(int seq) {
		this.seq = seq;
	}
	
	public boolean isNumeric() {
		return numeric;
	}
	
	public void setNumeric(boolean numeric) {
		this.numeric = numeric;
	}

	@Override
	public int compareTo(ColumnInfo o) {
		Integer s1 = Integer.valueOf(seq);
		Integer s2 = Integer.valueOf(o.getSeq());
		
		return s1.compareTo(s2);
	}
}
