package object.workorder;

public class MaterialCategory {
	private String code;
	private String naam;
	private int active;
	private String lastUpdate;
	
	public MaterialCategory(String code, String naam, int active, String lastUpdate) {
		this.setCode(code);
		this.setNaam(naam);
		this.setActive(active);
		this.setLastUpdate(lastUpdate);
	}
	
	public String getNaam() {
		return naam;
	}
	
	public void setNaam(String naam) {
		this.naam = naam;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public int getActive() {
		return active;
	}
	
	public void setActive(int active) {
		this.active = active;
	}
	
	public String getLastUpdate() {
		return lastUpdate;
	}
	
	public void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
