package object;

public class Token {
	protected String softwareToken;
	protected String consumerToken;
	protected String consumerSecret;
	protected String tempToken;
	protected String tempSecret;
	protected String accessToken;
	protected String accessSecret;
	protected String verifyToken;
	protected String softwareName;
	
	public Token(String cT, String cS, String aT, String aS, String sT, String sN) {
		this.consumerToken = cT;
		this.consumerSecret = cS;
		this.accessToken = aT;
		this.accessSecret = aS;
		this.softwareToken = sT;
		this.softwareName = sN;
	}
	
	public Token() {
		
	}
	
	public void setConsumerToken(String key) {
		this.consumerToken = key;
	}
	
	public String getConsumerToken() {
		return consumerToken;
	}
	
	public void setConsumerSecret(String key) {
		this.consumerSecret = key;
	}
	
	public String getConsumerSecret() {
		return consumerSecret;
	}
	
	public void setTempToken(String key) {
		this.tempToken = key;
	}
	
	public String getTempToken() {
		return tempToken;
	}
	
	public void setTempSecret(String key) {
		this.tempSecret = key;
	}
	
	public String getTempSecret() {
		return tempSecret;
	}
	
	public void setAccessToken(String key) {
		this.accessToken = key;
	}
	
	public String getAccessToken() {
		return accessToken;
	}
	
	public void setAccessSecret(String key) {
		this.accessSecret = key;
	}
	
	public String getAccessSecret() {
		return accessSecret;
	}
	
	public void setVerifyToken(String key) {
		this.verifyToken = key;
	}
	
	public String getVerifyToken() {
		return verifyToken;
	}
	
	public void setSoftwareToken(String key) {
		this.softwareToken = key;
	}
	
	public String getSoftwareToken() {
		return softwareToken;
	}
	
	public void setSoftwareName(String name) {
		this.softwareName = name;
	}
	
	public String getSoftwareName() {
		return softwareName;
	}
}
