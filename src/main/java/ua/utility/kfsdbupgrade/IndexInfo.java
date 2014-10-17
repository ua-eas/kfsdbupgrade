package ua.utility.kfsdbupgrade;

import java.util.HashSet;
import java.util.Set;

public class IndexInfo {
	private String indexName;
	private Set <String> indexColumns = new HashSet<String>();
	
	public IndexInfo(String indexName) {
		this.indexName = indexName;
	}
	
	public String getIndexName() {
		return indexName;
	}
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
	public Set<String> getIndexColumns() {
		return indexColumns;
	}
	public void setIndexColumns(Set<String> indexColumns) {
		this.indexColumns = indexColumns;
	}
	
	public void addColumn(String columnName) {
		indexColumns.add(columnName);
	}
}
