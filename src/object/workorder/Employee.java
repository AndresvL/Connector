package object.workorder;

public class Employee {
	private String firstName;
	private String lastName;
	private String code;
	
	public Employee(String fn, String ln, String code) {
		this.firstName = fn;
		this.lastName = ln;
		this.code = code;
		
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
}
