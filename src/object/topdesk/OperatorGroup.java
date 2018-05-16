package object.topdesk;

public class OperatorGroup {
	private String id;
	private String groupName;
	private String status;
	private String location;
	
	public OperatorGroup() {
		
	}
	
	public OperatorGroup(String id, String groupName) {
		this.id = id;
		this.groupName = groupName;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
}
