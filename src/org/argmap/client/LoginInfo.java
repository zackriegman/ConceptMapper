package org.argmap.client;

import java.io.Serializable;

public class LoginInfo implements Serializable {
	/** added to suppress warnings */
	private static final long serialVersionUID = 1L;
	public boolean loggedIn;
	public String firstName;
	public String lastName;
	public String nickName;
	public String email;
	public String logOutURL;
	public String logInURL;
	public boolean isAdmin;
}

