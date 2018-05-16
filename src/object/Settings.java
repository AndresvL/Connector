package object;

import java.io.Serializable;
import java.util.ArrayList;

public class Settings implements Serializable {
	private static final long serialVersionUID = 1L;
	private String importOffice, exportOffice, factuurType, user, exportWerkbontype, syncDate, materialCode;
	private ArrayList<String> importObjects, exportObjects;
	private int roundedHours;
	
	public Settings(String iO, String eO, String fT, ArrayList<String> iObj, String sU, String eWT, int rH, String sD,
			String mC, ArrayList<String> eR) {
		setImportOffice(iO);
		setExportOffice(eO);
		setFactuurType(fT);
		setImportObjects(iObj);
		setUser(sU);
		setExportWerkbontype(eWT);
		setRoundedHours(rH);
		setSyncDate(sD);
		setMaterialCode(mC);
		setExportObjects(eR);
	}
	
	public void setUser(String sU) {
		user = sU;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getImportOffice() {
		return importOffice;
	}
	
	public void setImportOffice(String importOffice) {
		this.importOffice = importOffice;
	}
	
	public String getExportOffice() {
		return exportOffice;
	}
	
	public void setExportOffice(String exportOffice) {
		this.exportOffice = exportOffice;
	}
	
	public String getFactuurType() {
		return factuurType;
	}
	
	public void setFactuurType(String factuurType) {
		this.factuurType = factuurType;
	}
	
	public ArrayList<String> getImportObjects() {
		return importObjects;
	}
	
	public void setImportObjects(ArrayList<String> importObjects) {
		this.importObjects = importObjects;
	}
	
	public String getExportWerkbontype() {
		return exportWerkbontype;
	}
	
	public void setExportWerkbontype(String exportWerkbontype) {
		this.exportWerkbontype = exportWerkbontype;
	}
	
	public int getRoundedHours() {
		return roundedHours;
	}
	
	public void setRoundedHours(int roundedHours) {
		this.roundedHours = roundedHours;
	}
	
	public String getSyncDate() {
		return syncDate;
	}
	
	public void setSyncDate(String syncDate) {
		this.syncDate = syncDate;
	}
	
	// Saves in settings table under 'user'
	public String getMaterialCode() {
		return materialCode;
	}
	
	public void setMaterialCode(String materialCode) {
		this.materialCode = materialCode;
	}
	
	public ArrayList<String> getExportObjects() {
		return exportObjects;
	}
	
	public void setExportObjects(ArrayList<String> exportObjects) {
		this.exportObjects = exportObjects;
	}
}
