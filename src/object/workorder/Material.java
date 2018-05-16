package object.workorder;

public class Material {
	private String code, unit;
	private String description;
	private double price;
	private String subCode;
	private String quantity;
	private String modified;
	private String id;
	
	public Material(String c, String subc, String u, String des, double p, String q, String m, String id) {
		this.code = c;
		this.subCode = subc;
		this.unit = u;
		this.description = des;
		this.price = p;
		this.quantity = q;
		this.modified = m;
		this.setId(id);
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getUnit() {
		if (unit.equals("")) {
			unit = "<empty>";
		}
		return unit;
	}
	
	public void setUnit(String unit) {
		this.unit = unit;
	}
	
	public String getDescription() {
		if (description.equals("")) {
			description = "<empty>";
		}
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public double getPrice() {
		return price;
	}
	
	public void setPrice(double price) {
		this.price = price;
	}
	
	public String getSubCode() {
		return subCode;
	}
	
	public void setSubCode(String subCode) {
		this.subCode = subCode;
	}
	
	public String getQuantity() {
		return quantity;
	}
	
	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}
	
	public String getModified() {
		return modified;
	}
	
	public void setModified(String modified) {
		this.modified = modified;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
}
