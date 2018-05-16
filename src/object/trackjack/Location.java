package object.trackjack;

public class Location {
	protected String latitude;
	protected String longitude;
	// Identifier
	protected String id;
	// 0 for employee, 1 for car
	protected int type;
	
	public Location(String lat, String lon, String id, int type) {
		this.latitude = lat;
		this.longitude = lon;
		this.id = id;
		this.type = type;
	}
	
	public String getLatitude() {
		return latitude;
	}
	
	public String getLongitude() {
		return longitude;
	}
	
	public String getId() {
		return id;
	}
	
	public int getType() {
		return type;
	}
}