
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.shareddata.Shareable;


public class LocalSession implements Shareable {

	private transient ServerWebSocket socket;

	private long maxIdleDuration = 20000;
	private long lastActivity;
	private final long sessionId;
	private final String userId;
	private boolean businessLogicAckReceived;

	public boolean isIdle() {
		return lastActivity + maxIdleDuration < System.currentTimeMillis();

	}
	

	public LocalSession(long sessionId,String userId) {
		super();
		this.lastActivity = System.currentTimeMillis();
		this.sessionId=sessionId;
		this.userId=userId;
	}


	/**
	 * @return
	 *         <p>
	 * 		release all resouces, closes connection, stop listners and other
	 *         periodic tasks and notify business layer about same
	 *         
	 *         <P>
	 *         it perform following task
	 *         <ul>
	 *         <li>notify business layer
	 *         <li> stop heart-beat
	 *         <li> clean resources
	 * 
	 */
	public boolean destroy() {
		return true;

	}
	
	public void initializeConnection(ServerWebSocket socket, Vertx vertx){
		this.socket=socket;
	}

	public ServerWebSocket getSocket() {
		return socket;
	}

	public void setSocket(ServerWebSocket socket) {
		this.socket = socket;
	}
	/**
	 * updates last activity time. This should be called whenever a new message
	 * arrives on socket
	 */
	public void touch() {
		lastActivity = System.currentTimeMillis();
	}


	public long getSessionId() {
		return sessionId;
	}
	public String getUserId() {
		return userId;
	}


	public boolean businessLogicAckReceived(){
		return businessLogicAckReceived;
	}
	public void businessLogicAckReceived(boolean ack){
		businessLogicAckReceived=ack;
	}

	@Override
	public String toString() {
		return "LocalSession [sessionId=" + sessionId + ", userId=" + userId + "]";
	}
}
