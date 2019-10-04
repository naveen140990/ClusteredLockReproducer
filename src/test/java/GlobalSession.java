import java.io.Serializable;

public class GlobalSession implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5037552566268020783L;
	private boolean valid;
	private long sessionId;
	private String metaData;
	private String bineraryHanderId;
	private String textHandler;
	private User user;
	

	public GlobalSession( long sessionId) {
		super();
		this.valid = false;
		this.sessionId = sessionId;
	}
	public GlobalSession( long sessionId,User user) {
		super();
		this.valid = user!=null;
		this.sessionId = sessionId;
		this.user=user;

	}
	
	//TODO: remove this
	@Deprecated
	public GlobalSession() {
		super();
		this.valid=true;
	}
	public boolean isValid() {
		return valid;
	}

	public long getSessionId() {
		return sessionId;
	}

	

	//TODO: make this immutable
	public void setBineraryHanderId(String bineraryHanderId) {
		this.bineraryHanderId = bineraryHanderId;
	}
	public void setTextHandler(String textHandler) {
		this.textHandler = textHandler;
	}
	public User getUser() {
		return user;
	}
	public String getMetaData() {
		return metaData;
	}
	
	public String getBineraryHanderId() {
		return bineraryHanderId;
	}
	public String getTextHandler() {
		return textHandler;
	}
	@Override
	public String toString() {
		return "GlobalSession [valid=" + valid + ", sessionId=" + sessionId + ", user=" + user + ", metaData="
				+ metaData + ", bineraryHanderId=" + bineraryHanderId + ", textHandler=" + textHandler + "]";
	}
	
	/*public ChatUser getUser() {
		return user;
	}*/


}
