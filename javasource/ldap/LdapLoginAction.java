package ldap;

import java.util.Map;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.AuthenticationRuntimeException;
import com.mendix.systemwideinterfaces.core.ISession;
import com.mendix.systemwideinterfaces.core.UserAction;

public class LdapLoginAction extends UserAction<Object> {
	private final String username;
	private final String password;
    private final String currentSessionId;

	public final static String SESSION_MANAGER_PARAM = "sessionManager";
	public final static String USER_NAME_PARAM = "userName";
	public final static String PASSWORD_PARAM = "password";
	public final static String LOCALE_PARAM = "locale";
	public final static String CURRENT_SESSION_ID_PARAM = "currentSessionId";

	public LdapLoginAction( Map<String, ? extends Object> params) {
		super(Core.createSystemContext());
		this.username = (String) params.get(USER_NAME_PARAM);
		this.password = (String) params.get(PASSWORD_PARAM);
		this.currentSessionId = (String) params.get(CURRENT_SESSION_ID_PARAM);
	}

	@Override
	public ISession executeAction() throws Exception {
		try {
			if (this.username == null || this.username.isEmpty() || this.password == null || this.password.isEmpty())
				throw new AuthenticationRuntimeException("LdapLogin FAILED: empty usernames or passwords are not allowed: " + this.username);
			return LdapModule.getInstance().authenticateUser(getContext(), this.username, this.password, this.currentSessionId);
		}
		catch (AuthenticationRuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().contains("sessions exceeded")) // #15861
				throw e;
			throw new AuthenticationRuntimeException("LdapLogin FAILED: error while checking login: " + e.getMessage());
		}
	}

	@Override
	public String toString() {
		return "[LdapLoginAction:: username: " + this.username + " password: " + "]";
	}

}
