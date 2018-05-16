package object.workorder;

public class WOAPart {
	// Required
	private String prtCode;
	private String prtName;
	private String prtSupplierNr;
	private String prtValue;
	// Optionel
	private String objImage;
	
	public WOAPart(String prtCode, String prtName, String supplierNr, String prtValue) {
		this.setPrtCode(prtCode);
		this.setPrtName(prtName);
		this.setPrtSupplierNr(supplierNr);
		this.setPrtValue(prtValue);
		this.setObjImage(prtValue);
	}
	
	public String getPrtCode() {
		return prtCode;
	}
	
	public void setPrtCode(String prtCode) {
		this.prtCode = prtCode;
	}
	
	public String getPrtName() {
		return prtName;
	}
	
	public void setPrtName(String prtName) {
		this.prtName = prtName;
	}
	
	public String getPrtSupplierNr() {
		return prtSupplierNr;
	}
	
	public void setPrtSupplierNr(String prtSupplierNr) {
		this.prtSupplierNr = prtSupplierNr;
	}
	
	public String getPrtValue() {
		return prtValue;
	}
	
	public void setPrtValue(String prtValue) {
		this.prtValue = prtValue;
	}
	
	public String getObjImage() {
		return objImage;
	}
	
	public void setObjImage(String objImage) {
		this.objImage = objImage;
	}
	
}