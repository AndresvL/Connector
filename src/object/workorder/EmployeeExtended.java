package object.workorder;

public class EmployeeExtended {
	private String firstName;
	private String lastName;
	private String code;
	private String mobile;
	private int id;
	
	public EmployeeExtended(int id, String fn, String ln, String code, String mob) {
		this.id = id;
		this.firstName = fn;
		this.lastName = ln;
		this.code = code;
		this.setMobile(mob);
		
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getMobile() {
		return mobile;
	}
	
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	
}
