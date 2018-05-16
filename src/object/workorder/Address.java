package object.workorder;

public class Address {
	private String name, phoneNumber, email, street, houseNumber, postalCode, city, remark, type;
	private int addressId;
	
	public Address() {
		
	}
	
	public Address(String name, String phoneNumber, String email, String street, String houseNumber, String postalCode,
			String city, String remark, String type, int addressId) {
		// contact
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.street = street;
		this.houseNumber = houseNumber;
		this.postalCode = postalCode;
		this.city = city;
		this.remark = remark;
		this.type = type;
		this.addressId = addressId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getStreet() {
		if (street.equals("") || street == null) {
			street = "<empty>";
		}
		return street;
	}
	
	public void setStreet(String street) {
		this.street = street;
	}
	
	public String getHouseNumber() {
		return houseNumber;
	}
	
	public void setHouseNumber(String houseNumber) {
		this.houseNumber = houseNumber;
	}
	
	public String getPostalCode() {
		if (postalCode.equals("") || postalCode == null) {
			postalCode = "<empty>";
		}
		return postalCode;
	}
	
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	
	public String getCity() {
		if (city.equals("") || city == null) {
			city = "<empty>";
		}
		return city;
	}
	
	public void setCity(String city) {
		this.city = city;
	}
	
	public String getRemark() {
		return remark;
	}
	
	public void setRemark(String remark) {
		this.remark = remark;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public int getAddressId() {
		return addressId;
	}
	
	public void setAddressId(int addressId) {
		this.addressId = addressId;
	}
}
