import java.io.Serializable;

/**
 * @author nishant
 *	this user will be preserved across cluster as user object
 */
public class User implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 4252291116589827186L;
	private final String id;
	private final String email;
	private String userName;

	public User(String id, String email, String userName) {
		super();
		this.id = id;
		this.email = email;
		this.userName = userName;
	}

	public String getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", email=" + email + "]";
	}

	public String getUserName() {
		return userName;
	}


}