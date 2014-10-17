package ua.utility.kfsdbupgrade;

import java.util.ArrayList;
import java.util.List;

public class TableIndexInfo {
	private String tableName;
	private List <IndexInfo> indexes = new ArrayList<IndexInfo>();
	private int maxIndexSuffix = 1;
	
	public TableIndexInfo(String tableName) {
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public List<IndexInfo> getIndexes() {
		return indexes;
	}
	public void setIndexes(List<IndexInfo> indexes) {
		this.indexes = indexes;
	}
	public int getMaxIndexSuffix() {
		return maxIndexSuffix;
	}
	public void setMaxIndexSuffix(int maxIndexSuffix) {
		this.maxIndexSuffix = maxIndexSuffix;
	}
	
	public void addIndexInfo(IndexInfo i) {
		indexes.add(i);
	}
}
