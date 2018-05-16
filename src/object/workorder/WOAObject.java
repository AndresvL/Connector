package object.workorder;

public class WOAObject {
	// Required
	private String objCode;
	private String objDebiteurNummer;
	private String objDescription;
	// Optionel
	private String supCode;
	private String parentObjCode;
	private String objImage;
	private String objFloorLevel;
	private String objLocation;
	private String objType;
	private String objModel;
	private String objBrand;
	private String objDateWarrantyExpires;
	private String objSerialnumber;
	private String objDateLastInspection;
	private String objDateInstallation;
	private String objFreefield1;
	private Double objPrice;
	
	public WOAObject(String code, String debtorNr, String desc) {
		this.setObjCode(code);
		this.setObjDebiteurNummer(debtorNr);
		this.setObjDescription(desc);
	}
	
	public WOAObject(String code, String debtorNr, String desc, String supCode, String parentObj, String image,
			String floorLever, String location, String type, String model, String brand, String warranty, String serial,
			String lastInspection, String installationDate, String freeField, String price) {
		this.setObjCode(code);
		this.setObjDebiteurNummer(debtorNr);
		this.setObjDescription(desc);
		this.setParentObjCode(parentObj);
		this.setObjImage(image);
		this.setObjFloorLevel(floorLever);
		this.setObjLocation(location);
		this.setObjType(type);
		this.setObjModel(model);
		this.setObjBrand(brand);
		this.setObjDateWarrantyExpires(warranty);
		this.setObjSerialnumber(serial);
		this.setObjDateLastInspection(lastInspection);
		this.setObjDateInstallation(installationDate);
		this.setObjFreefield1(freeField);
		this.setObjPrice(objPrice);
	}
	
	public String getObjCode() {
		return objCode;
	}
	
	public void setObjCode(String objCode) {
		this.objCode = objCode;
	}
	
	public String getObjDebiteurNummer() {
		return objDebiteurNummer;
	}
	
	public void setObjDebiteurNummer(String objDebiteurNummer) {
		this.objDebiteurNummer = objDebiteurNummer;
	}
	
	public String getObjDescription() {
		return objDescription;
	}
	
	public void setObjDescription(String objDescription) {
		this.objDescription = objDescription;
	}
	
	public String getSupCode() {
		return supCode;
	}
	
	public void setSupCode(String supCode) {
		this.supCode = supCode;
	}
	
	public String getParentObjCode() {
		return parentObjCode;
	}
	
	public void setParentObjCode(String parentObjCode) {
		this.parentObjCode = parentObjCode;
	}
	
	public String getObjImage() {
		return objImage;
	}
	
	public void setObjImage(String objImage) {
		this.objImage = objImage;
	}
	
	public String getObjFloorLevel() {
		return objFloorLevel;
	}
	
	public void setObjFloorLevel(String objFloorLevel) {
		this.objFloorLevel = objFloorLevel;
	}
	
	public String getObjLocation() {
		return objLocation;
	}
	
	public void setObjLocation(String objLocation) {
		this.objLocation = objLocation;
	}
	
	public String getObjType() {
		return objType;
	}
	
	public void setObjType(String objType) {
		this.objType = objType;
	}
	
	public String getObjModel() {
		return objModel;
	}
	
	public void setObjModel(String objModel) {
		this.objModel = objModel;
	}
	
	public String getObjBrand() {
		return objBrand;
	}
	
	public void setObjBrand(String objBrand) {
		this.objBrand = objBrand;
	}
	
	public String getObjDateWarrantyExpires() {
		return objDateWarrantyExpires;
	}
	
	public void setObjDateWarrantyExpires(String objDateWarrantyExpires) {
		this.objDateWarrantyExpires = objDateWarrantyExpires;
	}
	
	public String getObjSerialnumber() {
		return objSerialnumber;
	}
	
	public void setObjSerialnumber(String objSerialnumber) {
		this.objSerialnumber = objSerialnumber;
	}
	
	public String getObjDateLastInspection() {
		return objDateLastInspection;
	}
	
	public void setObjDateLastInspection(String objDateLastInspection) {
		this.objDateLastInspection = objDateLastInspection;
	}
	
	public String getObjDateInstallation() {
		return objDateInstallation;
	}
	
	public void setObjDateInstallation(String objDateInstallation) {
		this.objDateInstallation = objDateInstallation;
	}
	
	public String getObjFreefield1() {
		return objFreefield1;
	}
	
	public void setObjFreefield1(String objFreefield1) {
		this.objFreefield1 = objFreefield1;
	}
	
	public Double getObjPrice() {
		return objPrice;
	}
	
	public void setObjPrice(Double objPrice) {
		this.objPrice = objPrice;
	}
}