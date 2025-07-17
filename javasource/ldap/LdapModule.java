package ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import ldap.ImportUserRecord.Result;
import ldap.proxies.LdapCredentialsHistory;
import ldap.proxies.LdapDirectory;
import ldap.proxies.LdapPath;
import ldap.proxies.LdapServer;
import ldap.proxies.LookupUserEntity;
import ldap.proxies.ServerConfigType;
import ldap.proxies.ValidationFrequency;
import ldap.proxies.constants.Constants;

import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.InvalidNameException;
import org.springframework.ldap.UncategorizedLdapException;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.NameClassPairCallbackHandler;
import org.springframework.ldap.core.support.LdapContextSource;

import system.proxies.User;
import system.proxies.UserRole;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.AuthenticationRuntimeException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.ISession;
import com.mendix.systemwideinterfaces.core.IUser;
import com.mendix.systemwideinterfaces.core.UserAction;

import encryption.actions.DecryptString;

public class LdapModule {
	public static int PAGE_SIZE = 1000; // 1000 is active directory default:
										// http://support.microsoft.com/kb/315071

	public static final String MEMBER_OF_ATTR = "memberOf";
	public static final String ALL_FILTER = "(objectclass=*)";

	public static final String LDAP_LOG = "Ldap";

	private List<LdapServer> activeLdapServers;
	private Map<Long, List<String>> ignoredUserTypesPerServer = new HashMap<>();
	private boolean hasCreateConfigServer = false;
	private boolean customProvisioningEnabled = false;
	private LdapActionListener listener;
	protected static boolean usernameLowerCase = true; 

	private static LdapModule _self;

	private static final ILogNode log = Core.getLogger(LdapModule.LDAP_LOG);
	private static final ILogNode logNodeMaintenance = Core.getLogger("LdapCleanup");

	public static synchronized LdapModule getInstance() {
		if (_self == null)
			_self = new LdapModule();
		return _self;
	}

	/* Implemented module methods */

	/**
	 * Sets up the LDAP configuration with default values (any call to authenticate will fail until
	 * "setupLdapTemplate" has been called successfully)
	 */
	private LdapModule() {
		this.listener = new LdapActionListener();
		this.listener.addReplaceEvent(LdapLoginAction.class.getName());
		Core.addUserAction(LdapLoginAction.class);
		Core.getListenersRegistry().addListener(this.listener);

		try {
			MaintenanceAction action = new MaintenanceAction(Core.createSystemContext());
			Core.scheduleAtFixedRate(action, 10, 300, TimeUnit.SECONDS);
			logNodeMaintenance.info("Scheduling Ldap Cleanup action every 5 minutes");
		}
		catch (Exception e) {
			logNodeMaintenance.error("Unable to scheduling the Ldap Cleanup action", e);
		}
	}

	public List<String> getLdapUserAttributes(IContext context, LdapServer ldapServer) throws Exception {
		LdapTemplate template = getLdapTemplate(context, ldapServer, true);
		final List<String> attrs = new ArrayList<String>();

		for (LdapPath ldapPath : LdapPath.load(Core.createSystemContext(), "[" + LdapPath.MemberNames.LdapPath_LdapServer + "=" + ldapServer.getMendixObject().getId().toLong() + "]")) {
			// Use a page size of 1, stop after first page
			PagedResultsDirContextProcessor control = new PagedResultsDirContextProcessor(1, null);
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// MWE: note, class filter was * in LDAP 3!
			template.search(LdapModule.normalizeDn(ldapPath.getLocation()), ldapServer.getLDAPDirectoryUserSearchFilter(), searchControls, new AttributesMapper<Object>() {
				@Override
				public Object mapFromAttributes(Attributes arg0) throws NamingException {
					for (NamingEnumeration<String> e = arg0.getIDs(); e.hasMoreElements();)
						attrs.add(e.nextElement());
					return null;
				}
			}, control);

		}

		return attrs;
	}

	/* Helper methods */

	/**
	 * Tries to authenticate a user against one of the registered ldap servers that have
	 * "Use LDAP Login" enabled
	 * 
	 * @param context
	 * @param enteredUsername
	 * @param password
	 * @return
	 * @throws Exception
	 */
	public ISession authenticateUser(IContext context, String enteredUsername, String password, String currentSessionId) throws Exception {
		if( usernameLowerCase )
			enteredUsername = enteredUsername.toLowerCase();
		
		IUser user = Core.getUser(context, enteredUsername);
		if (user == null && !this.hasCreateConfigServer && !this.customProvisioningEnabled)
			throw new AuthenticationRuntimeException("Login FAILED: unknown local user " + enteredUsername + " , is the userbase in sync?");

		if (this.activeLdapServers == null) {
			MaintenanceAction action = new MaintenanceAction(Core.createSystemContext());
			Core.execute(action);

			if (this.activeLdapServers == null)
				throw new CoreException("The Ldap configuration has not been initialized, please check the configuration. ");
		}

		String adUsername;
		boolean authenticationResult = false;
		List<IMendixObject> ldapCredentialsHistory = null;
		IMendixObject credentialHistory;
		Result r = null;

		for (int i = 0; i < this.activeLdapServers.size() && authenticationResult == false; i++) {
			LdapServer ldapServer = this.activeLdapServers.get(i);
			String ldapServerAddress = ldapServer.getServerAddress();
			log.trace("Evaluating ldap server: " + ldapServerAddress + " trying to authenticate, server config: " + ldapServer.getServerConfigType());

			switch (ldapServer.getServerConfigType()) {
			case OnlyAuthenticateLDAPusers:
				if (authenticateUser(context, ldapServer, enteredUsername, password, false))
					authenticationResult = true;
				if (ldapServer.getCustomUserProvisioning()) {
			          if (ldapServer.getLoginSuffix() != null && !enteredUsername.endsWith(ldapServer.getLoginSuffix())) {
			            enteredUsername = enteredUsername + ldapServer.getLoginSuffix();
			          }
			          if (ldapServer.getLoginPrefix() != null && !!enteredUsername.startsWith(ldapServer.getLoginPrefix())) {
			            enteredUsername = ldapServer.getLoginPrefix() + enteredUsername;
			          }
			          ldap.proxies.microflows.Microflows.customLoginLogic(context, enteredUsername, authenticationResult,
			              ldapServer);
			          user = Core.getUser(context, enteredUsername);
			        }
				break;
			case ImportLDAPUsers:
				if (authenticateUser(context, ldapServer, enteredUsername, password, false))
					authenticationResult = true;

				//TODO: Change this function to allow more users
				if (authenticationResult && ldapServer.getSynchronizationMethod() == ldap.proxies.SynchronizationMethod.SynchronizeByPath) {
					r = ImportUserRecord.runImportUsersByPath(ldapServer, enteredUsername);
				} else if (authenticationResult && ldapServer.getSynchronizationMethod() == ldap.proxies.SynchronizationMethod.SynchronizeByGroup){
					r = ImportUserRecord.runImportUsersByGroup(ldapServer, enteredUsername);
				}

				if (r.hasImportErrors())
					throw r.getImportError();

				ldapCredentialsHistory = r.getCredentialHistory();
				if (ldapCredentialsHistory != null && ldapCredentialsHistory.size() > 0) {
					for( int j = 0; j < ldapCredentialsHistory.size() && !authenticationResult; j++) {
						credentialHistory = ldapCredentialsHistory.get(j);
						adUsername = (String) credentialHistory.getValue(context, LdapCredentialsHistory.MemberNames.ADUsername.toString());
						log.trace("Retrieved ldap server: " + ldapServerAddress + " username: " + enteredUsername + " mapped to ad username: " + adUsername + " from path: " + (String) credentialHistory.getValue(context, LdapCredentialsHistory.MemberNames.LdapPath.toString()) );

						if (authenticateUser(context, ldapServer, adUsername, password, false)) {
							ldapCredentialsHistory.remove(j);
							break;
						}
					}
					Core.delete(context, ldapCredentialsHistory);
				}
				else
					log.trace("No user found on ldap server: " + ldapServerAddress + " for username: " + enteredUsername);

				break;
			case CreateDuringSync:
				if (authenticateUser(context, ldapServer, enteredUsername, password, false))
					authenticationResult = true;

				if (authenticationResult && ldapServer.getSynchronizationMethod() == ldap.proxies.SynchronizationMethod.SynchronizeByPath) {
					r = ImportUserRecord.runImportUsersByPath(ldapServer, enteredUsername);
				} else if (authenticationResult && ldapServer.getSynchronizationMethod() == ldap.proxies.SynchronizationMethod.SynchronizeByGroup){
					r = ImportUserRecord.runImportUsersByGroup(ldapServer, enteredUsername);
				}
				
				if (r.hasImportErrors())
					throw r.getImportError();

				ldapCredentialsHistory = r.getCredentialHistory();
				if (ldapCredentialsHistory != null&& ldapCredentialsHistory.size() > 0) {
					for( int j = 0; j < ldapCredentialsHistory.size() && !authenticationResult; j++) {
						credentialHistory = ldapCredentialsHistory.get(j);
						adUsername = (String) credentialHistory.getValue(context, LdapCredentialsHistory.MemberNames.ADUsername.toString());
						log.trace("Retrieved ldap server: " + ldapServerAddress + " username: " + enteredUsername + " mapped to ad username: " + adUsername + " from path: " + (String) credentialHistory.getValue(context, LdapCredentialsHistory.MemberNames.LdapPath.toString()) );

						if (authenticateUser(context, ldapServer, adUsername, password, false)) {
							ldapCredentialsHistory.remove(j);
							break;
						}
					}
					Core.delete(context, ldapCredentialsHistory);
				}
				else
					log.trace("No user found on ldap server: " + ldapServerAddress + " for username: " + enteredUsername);

				user = Core.getUser(context, enteredUsername);
			}
		}

		// Evaluate a second time if the user is empty, if it is still empty no valid user could be
		// found and we need to abort
		if (user == null)
			throw new AuthenticationRuntimeException("Login FAILED: unknown local user " + enteredUsername + " , is the userbase in sync?");

		if (!authenticationResult)
			throw new AuthenticationRuntimeException("LdapLogin FAILED: invalid password for user " + enteredUsername);

		ISession session = null;
		User userObj = User.initialize(context, user.getMendixObject());

		if (userObj.getActive() && !userObj.getBlocked() && userObj.getUserRoles().size() > 0) {
			session = Core.initializeSession(user, currentSessionId);
		}
		else
			throw new AuthenticationRuntimeException("Login FAILED: user " + enteredUsername + " is inactive or blocked or has no userroles");

		if (session == null)
			throw new AuthenticationRuntimeException("LdapLogin FAILED: invalid password for user " + enteredUsername);

		return session;
	}


	/**
	 * Tries to authenticate a user against the ldap server
	 * 
	 * @param context
	 * @param username
	 * @param password
	 * @return
	 * @throws Exception
	 */
	public boolean authenticateUser(IContext context, LdapServer ldapServer, String username, String password, boolean isDebug) throws Exception {
		username = (ldapServer.getLoginPrefix() != null ? ldapServer.getLoginPrefix().trim() : "") + 
					username + 
					(ldapServer.getLoginSuffix() != null ? ldapServer.getLoginSuffix().trim() : ""); 

		log.trace("Trying to authenticate " + username + " with LDAP");
		if (authenticate(username, password, getLdapTemplate(context, ldapServer, isDebug), isDebug))
			return true;

		return false;
	}

	/**
	 * Authenticates a user against the LDAP server, as configured by setupLdapTemplate
	 * 
	 * @param username
	 * @param password
	 * @return
	 * @throws CoreException 
	 */
	private static boolean authenticate(String username, String password, LdapTemplate ldapTemplate, boolean throwException) throws CoreException {
		LdapContextSource cs = (LdapContextSource) ldapTemplate.getContextSource();
		cs.setUserDn(username);
		cs.setPassword(password);
		cs.setReferral("follow");
		cs.afterPropertiesSet();
		try {
			ldapTemplate.setContextSource(cs);
			cs.getReadOnlyContext();
		}
		//Only error.log communication or uncategorized exceptions
		catch ( CommunicationException | UncategorizedLdapException e1 ) {
			log.error("Failed to authenticate because of exception: " + e1.getMessage(), e1);
			if( throwException ) 
				throw new CoreException("Failed to authenticate because of exception: " + e1.getMessage(), e1);
			return false;
		}
		//For all standard Ldap errors only do trace logging so we can trace authentication when necessary, but we don't want to pollute the regular log file
		catch (Exception e) {
		    log.trace("Failed to authenticate because of exception: " + e.getMessage(), e);
			if( throwException ) 
				throw new CoreException("Failed to authenticate because of exception: " + e.getMessage(), e);
			return false;
		}
		return true;
	}

	/**
	 * Sets up the LDAP template to use new credentials and server configuration, and tries to
	 * authenticate
	 * 
	 * @param username
	 * @param password
	 * @param ldapServerAddress
	 * @param ldapLocation
	 * @param isActiveDirectory
	 * @throws Exception
	 *         if LDAP can't login
	 */
	private static LdapTemplate createLdapTemplate(String ldapUsername, String ldapPassword, String ldapServerAddress, boolean isActiveDirectory) throws Exception {
		
		// source: http://forum.spring.io/forum/spring-projects/data/ldap/48260-com-sun-jndi-ldap-connect-timeout-doesn-t-work
		// set time-out on all LDAP actions (default 10s = 10000ms)
		// added by JPU on Nov 11, 2016
		HashMap<String, Object> baseEnvProps = new HashMap<String, Object>();
		baseEnvProps.put("com.sun.jndi.ldap.connect.timeout", Constants.getConnectionTimeout());
		baseEnvProps.put("com.sun.jndi.ldap.read.timeout", Constants.getReadTimeout());
		
		LdapContextSource cs = new LdapContextSource();
		// Active directory throws partialresult exceptions because of referrals
		cs.setBase("");
		cs.setPassword(ldapPassword);
		cs.setUserDn(ldapUsername);
		cs.setUrl(ldapServerAddress);
		cs.setBaseEnvironmentProperties(baseEnvProps);
		cs.afterPropertiesSet();
		cs.setReferral("follow");
        
		LdapTemplate ldapTemplate = new LdapTemplate(cs);
		ldapTemplate.setIgnorePartialResultException(isActiveDirectory);
		
		if (authenticate(ldapUsername, ldapPassword, ldapTemplate, false) == false)
			throw new LdapException("Invalid ldap credentials");

		return ldapTemplate;
	}

	protected LdapTemplate getLdapTemplate(IContext context, LdapServer ldapServer, boolean isDebug) throws Exception {
		String ldapUsername = ldapServer.getUserName();
		String ldapPassword = getLdapPassword(context, ldapServer);
		String ldapServerAddress = ldapServer.getServerAddress();
		Boolean ldapIsActiveDirectory = ldapServer.getIsActiveDirectory();

		// LdapTemplate ldapTemplate = ldapConnections.containsKey(ldapServerAddress) ? ldapConnections.get(ldapServerAddress) : createLdapTemplate(ldapUsername, ldapPassword, ldapServerAddress, ldapIsActiveDirectory);
		LdapTemplate ldapTemplate = createLdapTemplate(ldapUsername, ldapPassword, ldapServerAddress, ldapIsActiveDirectory);
		if (!authenticate(ldapUsername, ldapPassword, ldapTemplate, isDebug))
			throw new LdapException("Invalid LDAP credentials");

		return ldapTemplate;
	}

	public static boolean isInvalidDn(String distinguishedName) {
		if ((distinguishedName.contains("\"")) || (distinguishedName.contains("\\="))) {
		    log.warn("Couldn't read entry: " + distinguishedName + ", skipping rest of this branch");
			return true;
		}

		return false;
	}

	private static String normalizeDn(String distinguishedName) {
		if (distinguishedName.contains("/"))
			// Replacing forward slash in DN
			return distinguishedName.replace("/", "\\/");

		return distinguishedName;
	}

	public void searchAD(IContext context, LdapServer ldapServer, String basePath, String filter, boolean retrieveObjects, boolean fullDepth, NameClassPairCallbackHandler handler) throws Exception {
		if (ldapServer.getIsPagedSearchAllowed())
			pagedSearch(context, ldapServer, basePath, filter, retrieveObjects, fullDepth, handler);
		else
			normalSearch(context, ldapServer, basePath, filter, handler);
	}

	private void normalSearch(IContext context, LdapServer ldapServer, String basePath, String filter, NameClassPairCallbackHandler handler) throws Exception {
		log.trace("Performing a normal search in directory: " + basePath + " with filter: " + filter);
		LdapTemplate template = getLdapTemplate(context, ldapServer, false);

		template.search(LdapModule.normalizeDn(basePath), filter, handler);
	}

	/**
	 * Since all ldap operations are not paged by default (including list), this utility wraps the
	 * usual stuff to a paged operation
	 * 
	 * @param ldapServer
	 * @param basePath
	 *        baseDN
	 * @param filter
	 *        optional search filter
	 * @param retrieveObjects
	 *        whether to retrieve all attributes of an object or just DN and object class
	 * @param handler
	 *        callback invoked for each result
	 * @throws Exception
	 */
	private void pagedSearch(IContext context, LdapServer ldapServer, String basePath, String filter, boolean retrieveObjects, boolean fullDepth, NameClassPairCallbackHandler handler) throws Exception {
		log.trace("Performing a paged search in directory: " + basePath + " with filter: " + filter);
		LdapTemplate template = getLdapTemplate(context, ldapServer, false);
		int searchScope = SearchControls.ONELEVEL_SCOPE;
		if (fullDepth) {
			searchScope = SearchControls.SUBTREE_SCOPE;
		}
		
		// retrieve users on path using paging..
		PagedResultsDirContextProcessor control = new PagedResultsDirContextProcessor(LdapModule.PAGE_SIZE, null);
        SearchControls searchControls = new SearchControls();
        searchControls.setReturningObjFlag(retrieveObjects);
        // MWE: one_level in LDAP 3!
        // Jonathan van Alteren: Changed to allow different scopes for different use cases
        searchControls.setSearchScope(searchScope);

        do {
			// MWE: note, class filter was * in LDAP 3!
	        template.search(LdapModule.normalizeDn(basePath), filter, searchControls, handler, control);
		} while (control.getCookie().getCookie() != null);
	}

	public List<IMendixObject> readDirectoryChildren(final IContext userContext, final LdapServer ldapServer, final String dn) throws Exception {
		if (ldapServer == null)
			throw new IllegalArgumentException();

		log.debug("Reading directories of '" + dn + "'..");
		
		final List<IMendixObject> res = new ArrayList<IMendixObject>();

		if (isInvalidDn(dn))
			return res;

		try { 
			searchAD(userContext, ldapServer, normalizeDn(dn), ALL_FILTER, false, false, new NameClassPairCallbackHandler() {
				@Override
				public void handleNameClassPair(NameClassPair arg0) {
					try {
							log.debug("Found " + arg0.getNameInNamespace());
	
						LdapDirectory ld = new LdapDirectory(userContext); // Needs usercontext,
																			// completely visible
																			// otherwise
						ld.setDistinguishedName(arg0.getNameInNamespace());
						ld.setLdapDirectory_LdapServer(ldapServer);
						ld.commit();
						res.add(ld.getMendixObject());
					}
					catch (UnsupportedOperationException e) {
							log.error("Failed to determine name of child in " + dn, e);
					}
					catch (CoreException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
		catch (InvalidNameException e) {
			throw new CoreException( "An exception occured while reading the directory, did you specify a valid root directory?", e);
		}

		return res;
	}

	public void testConnection(IContext context, LdapServer ldapServer) throws Exception {
		getLdapTemplate(context, ldapServer, true);
	}

	public static String getLdapPassword(IContext context, LdapServer ldapServer) throws Exception {
		String unEncryptedPass = ldapServer.getPassword();
        String encryptedPass = ldapServer.getPassword_Encrypted();
        
		if (unEncryptedPass != null && !unEncryptedPass.equals("")) {
			throw new RuntimeException("Unencrypted password found in Ldap server configuration '" 
			        + ldapServer.getServerAddress() + "', please re-enter password in configuration.");
		} else if (encryptedPass == null || encryptedPass.equals("")) {
		    throw new RuntimeException("No encrypted password found in Ldap server configuration '" 
	                    + ldapServer.getServerAddress() + "', please re-enter password in configuration.");
		} else {
		    return encryption.proxies.microflows.Microflows.decrypt(context, encryptedPass);
		}
	}

	public void startMaintenance() {
		IContext context = Core.createSystemContext();
		MaintenanceAction action = new MaintenanceAction(context);
		Core.execute(action);
	}

	public class MaintenanceAction extends UserAction<Boolean> {

		public MaintenanceAction(IContext paramIContext) {
			super(paramIContext);
		}

		@Override
		public Boolean executeAction() throws Exception {
			logNodeMaintenance.debug("Start maintenance");

			IContext context = getContext();
			 
			LdapModule.this.activeLdapServers = LdapServer.load(context, "[" + LdapServer.MemberNames.Enabled + "=true()]");
			
			/*
			 * Analyse all ldap servers which need to be cleaned up.
			 */
			logNodeMaintenance.trace("Found: " + LdapModule.this.activeLdapServers.size() + " ldapservers which need to be checked");
			boolean createServerConfig = false, customProvisioningEnabled = false;
			for (LdapServer ldapServer : LdapModule.this.activeLdapServers) {
				ServerConfigType type = ldapServer.getServerConfigType();

				List<LookupUserEntity> ignoredUsers = ldapServer.getLdapServer_IgnoreLookupUserEntity();
				if (ignoredUsers != null && ignoredUsers.size() > 0) {
					long ldapId = ldapServer.getMendixObject().getId().toLong();
					if (!LdapModule.this.ignoredUserTypesPerServer.containsKey(ldapId))
						LdapModule.this.ignoredUserTypesPerServer.put(ldapId, new ArrayList<String>());
					List<String> userList = LdapModule.this.ignoredUserTypesPerServer.get(ldapId);
					userList.clear();
					for (LookupUserEntity user : ignoredUsers)
						userList.add(user.getName());
				}
				else
					LdapModule.this.ignoredUserTypesPerServer.remove(ldapServer.getMendixObject().getId().toLong());

				switch (type) {

				case CreateDuringSync:
					createServerConfig = true;
					//$FALL-THROUGH$
				case ImportLDAPUsers:
					// When the ldap module is only handled for authentication and when we create
					// the login we need to configure how often we want to do the validation
				    if (ldapServer.getValidationFrequency() == ValidationFrequency.OnlyOnce) {
				        List<LdapCredentialsHistory> credentialsList = LdapCredentialsHistory.load(context, 
						        "[" + LdapCredentialsHistory.MemberNames.LdapCredentialsHistory_LdapServer + "=" + ldapServer.getMendixObject().getId().toLong() + "]");
				        Collection<? extends ISession> sessions = Core.getActiveSessions();
						logNodeMaintenance.trace("Found: " + credentialsList.size() + " credentials within the ldap server");

						/*
						 * Analyse the credentials list, check if the user session is still active.
						 * If not remove the credentials
						 */
						for (LdapCredentialsHistory credential : credentialsList) {
							String username = credential.getEnteredUsername();
							if (username == null)
							    credential.delete();
							else {
								boolean stillLoggedIn = false;
								for (ISession session : sessions) {
									if (username.equals(session.getUser(context).getName())) {
										stillLoggedIn = true;
										break;
									}
								}

								if (!stillLoggedIn)
									credential.delete();
							}
						}
					}
					break;
				case OnlyAuthenticateLDAPusers:
					customProvisioningEnabled = ldapServer.getCustomUserProvisioning();
					break;
				}
			}
			LdapModule.this.hasCreateConfigServer = createServerConfig;
			LdapModule.this.customProvisioningEnabled = customProvisioningEnabled;
			logNodeMaintenance.debug("Finished cleaning up credentials");

			return true;
		}

	}

	public boolean skipUserType(IUser user) throws CoreException {
		if (user == null)
			return false;
		
		//Skip when processing MxAdmin
		if( Core.getConfiguration().getAdminUserName().equals(user.getName()) ) 
			return true;
		
		if (this.activeLdapServers == null) {
            MaintenanceAction action = new MaintenanceAction(Core.createSystemContext());
            Core.execute(action);

            if (this.activeLdapServers == null)
                throw new CoreException("The Ldap configuration has not been initialized, please check the configuration. ");
        }

		boolean ignoreForAllServers = true;
		for (LdapServer ldapServer : this.activeLdapServers) {
			if (!skipUserType(user, ldapServer)) {
				
				if( ldapServer.getLdapServerByPass_UserRole() != null ) {
					Set<String> selectedRole = user.getUserRoleNames();
					boolean hasRoleToSkip = false;
					for( UserRole role : ldapServer.getLdapServerByPass_UserRole() ) {
						if( selectedRole.contains(role.getName() ) ) {
							hasRoleToSkip = true;
							break;
						}
					}
					if( !hasRoleToSkip && ignoreForAllServers ) {
						ignoreForAllServers = false;
						break;
					}
				}
				else { 
					ignoreForAllServers = false;
					break;
				}
			}
			

			
		}

		return ignoreForAllServers;
	}

	private boolean skipUserType(IUser user, LdapServer ldapServer) {
		List<String> ignoredUsers = this.ignoredUserTypesPerServer.get(ldapServer.getMendixObject().getId().toLong());
		if (ignoredUsers != null)
			return ignoredUsers.contains(user.getMendixObject().getType());

		return false;
	}
	
	public LdapActionListener getListener() {
		return this.listener;
	}
}
