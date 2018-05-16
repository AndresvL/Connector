package object.topdesk;

public class ProcessingStatus {
	private String id;
	private String name;
	private String onHold;
	private String processingState;
	private boolean value = false;
	
	public ProcessingStatus(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public ProcessingStatus(String id, String name, boolean value) {
		this.id = id;
		this.name = name;
		this.value = value;
	}
	
	public ProcessingStatus() {
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getOnHold() {
		return onHold;
	}
	
	public void setOnHold(String onHold) {
		this.onHold = onHold;
	}
	
	public String getProcessingState() {
		return processingState;
	}
	
	public void setProcessingState(String processingState) {
		this.processingState = processingState;
	}
	
	public boolean isValue() {
		return value;
	}
	
	public void setValue(boolean value) {
		this.value = value;
	}
	
}
