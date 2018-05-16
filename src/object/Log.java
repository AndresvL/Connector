package object;

public class Log {
	private String messages = null;
	private String timestamp;
	
	public Log(String messages, String timestamp) {
		this.setMessages(messages);
		this.setTimestamp(timestamp);
	}
	
	public String getMessages() {
		return messages;
	}
	
	public void setMessages(String messages) {
		this.messages = messages;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
}
