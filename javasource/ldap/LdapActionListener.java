/**
 * 
 */
package ldap;

import ldap.proxies.LdapServer;

import org.apache.commons.text.StringEscapeUtils;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.core.action.user.LoginAction;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.UserActionListener;

/**
 * Standard Commit listener
 */
public class LdapActionListener extends UserActionListener<LoginAction> {
    private static final ILogNode log = Core.getLogger(LdapModule.LDAP_LOG);

	/**
	 * @param targetClass
	 */
	public LdapActionListener() {
		super(LoginAction.class);
	}

	/**
	 * This action validates if the Ldap login action should be executed, when the result is true
	 * the ldap login action will be executed
	 */
	@Override
	public boolean check(LoginAction action) {
		if (action == null)
			throw new IllegalArgumentException("Action should not be null");

		IContext context = Core.createSystemContext();
		try {

			if (skipLdapAuthentication(context, action.getUserName()))
				return false;
			String xpath = "count(//" + LdapServer.getType() + "[" + LdapServer.MemberNames.Enabled + "=true()])";
			return Core.createXPathQuery(xpath).executeAggregateLong(context) > 0;

		}
		catch (CoreException e) {

		    log.error("Error while counting number of LDAP servers", e);
			return false; // do not replace

		}
	}

	/**
	 * Checks to see if the specified user has an MxAdministrator UserRole
	 * 
	 * @param username
	 * @throws CoreException
	 */
	public boolean skipLdapAuthentication(IContext c, String username) throws CoreException {
		
		// JPU - 2016-May-05
		// added to prevent XPath injection (ticket 465640)
		String escapedUsername = StringEscapeUtils.escapeXml10(username);
		
		if (LdapModule.getInstance().skipUserType(Core.getUser(c, escapedUsername))) {
		    log.info("Bypassing LDAP authentication for '" + escapedUsername + "'");

			return true;
		}

		return false;
	}

}
