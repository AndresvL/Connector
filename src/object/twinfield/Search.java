package object.twinfield;

public class Search {
	private String type;
	private String pattern;
	private int field = 0;
	private int maxRows = 0;
	private int firstRow = 0;
	private String[][] options;
	
	public Search(String t, String pat, int f, int first, int max, String[][] op) {
		this.setType(t);
		this.setPattern(pat);
		this.setField(f);
		this.setFirstRow(first);
		this.setMaxRows(max);
		this.setOptions(op);
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getPattern() {
		return pattern;
	}
	
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	public int getField() {
		return field;
	}
	
	public void setField(int field) {
		this.field = field;
	}
	
	public int getMaxRows() {
		return maxRows;
	}
	
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}
	
	public String[][] getOptions() {
		return options;
	}
	
	public void setOptions(String[][] options) {
		this.options = options;
	}
	
	public int getFirstRow() {
		return firstRow;
	}
	
	public void setFirstRow(int firstRow) {
		this.firstRow = firstRow;
	}
}
