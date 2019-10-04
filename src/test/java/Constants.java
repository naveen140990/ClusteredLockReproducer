public interface Constants {

	String SEND_ADMIN_MESSAGE="chat.amqp.admin_message.send";
	String SEND_MESSAGE_QUEUE="chat.amqp.message.send";
	String SEND_AUTHORIZATION_MESSAGE_QUEUE="chat.amqp.authorization.message.send";
	String SEND_SMALL_BINARY_MESSAGE_QUEUE="chat.sqs.message.send";
	String SEND_BIG_BINARY_MESSAGE_QUEUE="chat.sqs.message.big.send";
	String CRASHED_NODE_USER_MSG="chat.connection.crashedNode.users";
	

	public static String ACK_MSG_RECEIVED_LISTNER(String userId){
		return "chat.user.ack."+userId;
	}
	
	String GLOBAL_USER_SESSION_MAP_NAME = "global_user_session";
	String LOCAL_USER_SESSION_MAP_NAME = "local_user_session";
	String SESSION_COUNTER_NAME = "session_counter";
	String DIAGNOSTIC_TIMER_ID_MAP = "diagnostic_timer_id";
	String CLUSTER_WISE_USER_MAP = "cluster_wise_user_map";
	String PROXY_ID_PREFIX="chat.user.proxy.";
	String PONG_PREFIX="chat.user.pong";
	String USER_CLEAN_UP = "user.cleanup.session.id.";
}
