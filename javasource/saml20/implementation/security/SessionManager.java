package saml20.implementation.security;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.externalinterface.connector.RequestHandler;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.ISession;
import com.mendix.systemwideinterfaces.core.IUser;
import mxmodelreflection.proxies.Microflows;
import mxmodelreflection.proxies.MxObjectMember;
import mxmodelreflection.proxies.MxObjectType;
import org.opensaml.saml.common.SAMLException;
import saml20.implementation.SAMLFeedbackException;
import saml20.implementation.SAMLRequestContext;
import saml20.implementation.SAMLRequestHandler;
import saml20.implementation.common.Constants;
import saml20.implementation.common.Constants.SAMLAction;
import saml20.implementation.common.HTTPUtils;
import saml20.implementation.common.MendixUtils;
import saml20.implementation.common.SAMLUtil;
import saml20.implementation.provisioning.UserProvisioningHelper;
import saml20.implementation.wrapper.MxSAMLAssertion;
import saml20.proxies.ClaimMap;
import saml20.proxies.SAMLRequest;
import saml20.proxies.SAMLSession;
import saml20.proxies.SSOConfiguration;
import system.proxies.TokenInformation;
import system.proxies.User;
import system.proxies.UserRole;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {

	public static final String HTTP_HEADER_USER_AGENT = "User-Agent";
	public static final String AUTH_TOKEN_COOKIE_NAME = "AUTH_TOKEN";

	private static ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

	private Map<UUID, SAMLSessionInfo> activeSessions = new ConcurrentHashMap<UUID, SAMLSessionInfo>();

	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public class Configuration {

		private final String userEntityName;
		private final String userPrincipleMemberName;
		private boolean createUsers;
		private IMendixObject defaultUserRoleObject;
		private boolean useCustomUserProvisioning;
		private boolean useCustomAfterSigninLogic;
		
		// Changes are customVerifyMicroflow, customProvisioningMicroflow, customAfterSigninMicroflow
		private String customEvaluateInSessionAuthNMicroflow;
		private String customProvisioningMicroflow;
		private String customAfterSigninMicroflow;
		private boolean enableMobileAuthToken;
		protected boolean allowDelegatedAuthentication;
		private List<IMendixObject> mxClaimMapList;

		@SuppressWarnings("serial")
		public Configuration(IContext context, String userEntityName, String userPrincipleMemberName, boolean createUsers, boolean useUserProvisioning,
							 boolean useCustomAfterSigninLogic, String customProvisioningMicroflow, String customAfterSigninMicroflow, String customEvaluateInSessionAuthNMicroflow, boolean allowDelegatedAuthentication, boolean enableMobileAuthToken, IMendixObject defaultUserRoleObject, IMendixObject ssoConfigObj ) {
			this.userEntityName = userEntityName;
			this.userPrincipleMemberName = userPrincipleMemberName;
			this.createUsers = createUsers;
			this.useCustomUserProvisioning = useUserProvisioning;
			this.useCustomAfterSigninLogic = useCustomAfterSigninLogic;
			this.customProvisioningMicroflow = customProvisioningMicroflow;
			this.customAfterSigninMicroflow = customAfterSigninMicroflow;
			this.customEvaluateInSessionAuthNMicroflow = customEvaluateInSessionAuthNMicroflow;
			this.enableMobileAuthToken = enableMobileAuthToken;
			this.defaultUserRoleObject = defaultUserRoleObject;
			this.allowDelegatedAuthentication = allowDelegatedAuthentication;

			/*
			 * Retrieve the full claim map with all the attribute mapping fields
			 */
			this.mxClaimMapList = MendixUtils.retrieveFromDatabase(context, "//%s[%s = $id][%s != empty][%s != empty]",
					new HashMap<String, Object>() {{
						put("id", ssoConfigObj.getId());
					}},
					ClaimMap.entityName,
					ClaimMap.MemberNames.ClaimMap_SSOConfiguration.toString(),
					ClaimMap.MemberNames.ClaimMap_Attribute.toString(),
					ClaimMap.MemberNames.ClaimMap_MxObjectMember.toString()
			);
		}
	}

	private HashMap<String, Configuration> configurationSet = new HashMap<String, Configuration>();

	private static SessionManager _instance = null;

	public static SessionManager getInstance( IContext context ) throws SAMLException {
		if ( _instance == null )
			_instance = new SessionManager(context);

		return _instance;
	}

	private SessionManager( IContext context ) throws SAMLException {
		this.executor.scheduleAtFixedRate(this.periodicTask, 1, 1, TimeUnit.MINUTES);

		init(context, null);
	}

	public SessionManager init( IContext context, List<IMendixObject> ssoConfigurationList ) throws SAMLException {
		try {
			String userEntityName, userPrincipleMemberName;
			IMendixObject defaultUserRoleObject;
			boolean createUsers = false,
					useCustomUserProvisioning = false,
					useCustomAfterSignIn = false,
					enableDelegatedAuth = false,
					enableMobileAuthToken = false;
			String customProvisioningMicroflow = null,
					customAfterSigninMicroflow = null,
					customEvaluateInSessionAuthNMicroflow = null;

			if ( ssoConfigurationList != null ) {
				for( IMendixObject ssoConfigObj : ssoConfigurationList ) {
					String entityId = SAMLUtil.getEntityIdForConfig(context, ssoConfigObj);
					if ( entityId != null ) {
						SSOConfiguration ssoConfig = SSOConfiguration.initialize(context, ssoConfigObj);
						// String entityAlias = ssoConfig.getAlias();

						/*
						 * Retrieve the entity type for the user that we want to use, and the username which we use to
						 * compare the principle name
						 */
						MxObjectType mxObjectType = ssoConfig.getSSOConfiguration_MxObjectType();
						if ( mxObjectType != null )
							userEntityName = mxObjectType.getCompleteName();
						else
							userEntityName = null;

						MxObjectMember mxObjectMember = ssoConfig.getSSOConfiguration_MxObjectMember();
						if ( mxObjectMember != null )
							userPrincipleMemberName = mxObjectMember.getAttributeName();
						else
							userPrincipleMemberName = null;

						UserRole role = ssoConfig.getSSOConfiguration_DefaultUserRoleToAssign();
						if ( role != null )
							defaultUserRoleObject = role.getMendixObject();
						else
							defaultUserRoleObject = null;

						Microflows provisioningMicroflow = ssoConfig.getSSOConfiguration_CustomUserProvisioningMicroflow();
						if (provisioningMicroflow != null)
							customProvisioningMicroflow = provisioningMicroflow.getCompleteName();
						else
							customProvisioningMicroflow = null;

						Microflows afterSigninMicroflow = ssoConfig.getSSOConfiguration_CustomAfterSigninMicroflow();
						if (afterSigninMicroflow != null)
							customAfterSigninMicroflow = afterSigninMicroflow.getCompleteName();
						else
							customAfterSigninMicroflow = null;

						
						Microflows evaluateInSessionAuthNMicroflow = ssoConfig.getSSOConfiguration_CustomEvaluateInSessionAuthenticationMicroflow();
						if (evaluateInSessionAuthNMicroflow != null)
							customEvaluateInSessionAuthNMicroflow = evaluateInSessionAuthNMicroflow.getCompleteName();
						else
							customEvaluateInSessionAuthNMicroflow = null;


						createUsers = ssoConfig.getCreateUsers();
						useCustomUserProvisioning = ssoConfig.getUseCustomLogicForProvisioning();
						useCustomAfterSignIn = ssoConfig.getUseCustomAfterSigninLogic();
						enableDelegatedAuth = ssoConfig.getEnableDelegatedAuthentication();
						enableMobileAuthToken = ssoConfig.getEnableMobileAuthToken();

						this.configurationSet.put(entityId, new Configuration(context, userEntityName, userPrincipleMemberName, createUsers,
								useCustomUserProvisioning, useCustomAfterSignIn, customProvisioningMicroflow, customAfterSigninMicroflow, customEvaluateInSessionAuthNMicroflow, enableDelegatedAuth, enableMobileAuthToken, defaultUserRoleObject, ssoConfigObj));
					}
				}
			}
		}
		catch( CoreException e ) {
			throw new SAMLException(e);
		}

		return this;
	}

	Runnable periodicTask = new Runnable() {

		@Override
		public void run() {
			// Invoke method(s) to do the work
			evaluateActiveSessions();
		}
	};

	/**
	 * this method can be used to initialize an XAS session when the username is known and verified.
	 *
	 * @param entityId
	 * @param samlContext
	 * @param samlSession
	 * @param correspondingSAMLRequest
	 * @param entityAlias
	 * @param relayState
	 * @throws Exception
	 */
	public ISession createSession( String entityId, SAMLRequestContext samlContext, SAMLSessionInfo samlSession, SAMLRequest correspondingSAMLRequest, String entityAlias, String relayState ) throws Exception {
		IMxRuntimeResponse response = samlContext.getResponse();

		try {
			_logNode.debug("Initializing new session for user '" + samlSession.getIUser().getName() + "'");

			Configuration config = this.configurationSet.get(entityId);

			IContext context = samlContext.getIContext();

			IMendixObject currentSession = null, newSession = null, currentUser = null, newUser = null;
			if( config.useCustomAfterSigninLogic && samlContext.getCurrentSession() != null ) {
				currentSession = samlContext.getCurrentSession().getMendixObject();
				currentUser = samlContext.getCurrentSession().getUser(context).getMendixObject();
			}

			String previousSessionID = null;
			if (samlContext.getCurrentSession() != null) {
				previousSessionID = samlContext.getCurrentSession().getId().toString();
			};

			ISession session = Core.initializeSession(samlSession.getIUser(), previousSessionID);

			if( config.useCustomAfterSigninLogic ) {
				newSession = session.getMendixObject();
				newUser = session.getUser(context).getMendixObject();

				IContext mxContext = Core.createSystemContext();
				if (config.customAfterSigninMicroflow != null && !config.customAfterSigninMicroflow.equals("")) {
					Core.microflowCall(config.customAfterSigninMicroflow)
							.inTransaction(true)
							.withParam("PreviousSession", (currentSession == null ? null : system.proxies.Session.initialize(mxContext, currentSession).getMendixObject()))
							.withParam("PreviousUser", (currentUser == null ? null : User.initialize(mxContext, currentUser).getMendixObject()))
							.withParam("NewSession", system.proxies.Session.initialize(mxContext, newSession).getMendixObject())
							.withParam("NewUser", User.initialize(mxContext, newUser).getMendixObject())
							.withParam("SamlRequest", correspondingSAMLRequest.getMendixObject())
							.execute(mxContext);
				} else {
					saml20.proxies.microflows.Microflows.customAfterSigninLogic(Core.createSystemContext(),
							(currentSession == null ? null : system.proxies.Session.initialize(mxContext, currentSession)), system.proxies.Session.initialize(mxContext, newSession),
							(currentUser == null ? null : User.initialize(mxContext, currentUser)), User.initialize(mxContext, newUser), correspondingSAMLRequest );
				}
			}

			/**
			 * create cookies and redirect: String key, String value, String path, String domain, int expiry
			 */
			Core.addMendixCookies(samlContext.getRequest(),response,session,false);
			response.addHeader(RequestHandler.CSRF_TOKEN_HEADER, session.getCsrfToken());

			// Create authentication token for use in mobile apps
			if (config.enableMobileAuthToken) {
				String token = UUID.randomUUID().toString();
			    TokenInformation tokenInformation = new TokenInformation(samlContext.getIContext());
				tokenInformation.setToken(token);
				tokenInformation.setUserAgent(samlContext.getRequest().getHeader(HTTP_HEADER_USER_AGENT));
			    tokenInformation.setTokenInformation_User(User.initialize(samlContext.getIContext(), samlSession.getIUser().getMendixObject()));
			    tokenInformation.commit();
			    String authToken = token + ":" + samlSession.getIUser().getName();
				response.addCookie(AUTH_TOKEN_COOKIE_NAME, authToken,true);
			}
		    // end token generation

			// Third variable of addCookie is "Path" (based on source code) and setting this to "/" caused double
			// quotation marks to be added in the cloud (Linux?).
			// setting it to File.pathSeparator didn't resolve the issue in the Cloud. Removing the slash altogether did
			// resolve the issue.
			// -- JPU, 20150624
			String originURI = Constants._getInstance().SSO_PATH + SAMLAction.login + "?" + URLEncoder.encode(entityAlias, "UTF-8");
			response.addCookie("originURI",  originURI, "", "", Constants.COOKIE_SECONDS_PER_YEAR,true);

			cleanupSessions(samlSession, session);
			redirectUser(relayState, response);


			return session;
		}
		catch( Exception e ) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			throw new Exception("Single Sign On unable to create new session: " + e.getMessage(), e);
		}
	}

	/**
	 * From Mx10.13 onwards runtime support addCookie in core api, runtime will add needed cookies in this method
	 * @return true if runtime version from Mx10.13 otherwise false
	 */
	private  boolean isSupportAddCookieInCoreAPI() {
		String[] mxVersion = Core.getRuntimeVersion().split("\\.");
	     return	(Integer.parseInt(mxVersion[0]) >  10 || Integer.parseInt(mxVersion[0]) ==  10 && Integer.parseInt(mxVersion[1]) >= 13);
	}

	private void cleanupSessions(SAMLSessionInfo samlSession, ISession session) {
		UUID sessionIdTMP = samlSession.getSessionId();
		UUID sessionId = session != null ? session.getId() : null;
		// Remove the old sessionId reference from the map
		if (sessionIdTMP != null) {
			// can be null apparently (ticket #46552) so added check to avoid a nullpointerexception - JPU (Nov 16)
			this.activeSessions.remove(sessionIdTMP);
		}
		if (sessionId != null) {
			this.activeSessions.put(sessionId, samlSession);
		}
		_logNode.trace("Updating User session: '" + samlSession.getIUser().getName() + "', from SessionId: " + sessionIdTMP + " to " + sessionId);

		// The lock should be released so the session will be removed whenever the user logs out.
		samlSession.releaseLock();
	}

	private void redirectUser(String relayState, IMxRuntimeResponse response) throws UnsupportedEncodingException {
		// Determine where to redirect the user (either home/landing page or continuation URL)
		String redirectTo = Constants._getInstance().getSP_URI() + Constants.getLandingPage();
		String continuation = SAMLUtil.getContinuationFromRelayState(relayState);
		if (continuation != null && !continuation.equals("")) {
			if (saml20.proxies.constants.Constants.getDisableDecoding_Deeplink_URL()) {
				redirectTo = Constants._getInstance().getSP_URI() + continuation;
			} else {
				redirectTo = Constants._getInstance().getSP_URI() + URLDecoder.decode(continuation, "UTF-8");
			}

		}

		HTTPUtils.redirect(response, redirectTo);
	}

	
	// Executing customVerifyMicroflow
	public void executeVerifyMicroflow( String entityId, SAMLRequestContext samlContext, SAMLSessionInfo samlSession, SAMLRequest correspondingSAMLRequest, String entityAlias, String relayState ) throws Exception {
		IMxRuntimeResponse response = samlContext.getResponse();

		try {
			IUser iUser = samlSession.getIUser();
			_logNode.debug("Executing verify microflow for user '" + iUser.getName() + "'");

			Configuration config = this.configurationSet.get(entityId);
			IContext context = samlContext.getIContext();
			IMendixObject currentSession = null;
			IMendixObject currentUser = null;
			if (samlContext.getCurrentSession() != null) {
				currentSession = samlContext.getCurrentSession().getMendixObject();
				currentUser = samlContext.getCurrentSession().getUser(context).getMendixObject();
			}
			IContext mxContext = Core.createSystemContext();
			if (config.customEvaluateInSessionAuthNMicroflow != null && !config.customEvaluateInSessionAuthNMicroflow.equals("")) {
				Core.microflowCall(config.customEvaluateInSessionAuthNMicroflow)
						.inTransaction(true)
						.withParam("ExistingSession", (currentSession == null ? null : system.proxies.Session.initialize(mxContext, currentSession).getMendixObject()))
						.withParam("ExistingUser", (currentUser == null ? null : User.initialize(mxContext, currentUser).getMendixObject()))
						.withParam("NewUser", iUser.getMendixObject())
						.withParam("SamlRequest", correspondingSAMLRequest.getMendixObject())
						.execute(mxContext);
			} else {
				_logNode.warn("Verify attempted, but no custom verify microflow is configured in the application constants");
			}

			cleanupSessions(samlSession, null);
			redirectUser(relayState, response);
		}
		catch( Exception e ) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			throw new Exception("Single Sign On unable to create new session: " + e.getMessage(), e);
		}
	}
	public SAMLSessionInfo createSAMLSessionInfo(String entityId, String principalValue, HashMap<String, Object> assertionAttributes, SSOConfiguration ssoConfig, MxSAMLAssertion mxSAMLAssertion ) throws Exception {
		Configuration config = this.configurationSet.get(entityId);
		if (config == null) {
			_logNode.debug("No valid SSO Configuration could be found for the provided Entity ID: " + entityId);
			throw new SAMLFeedbackException("No valid SSO Configuration could be found for the provided Entity ID.")
					.addFeedbackMessage(Constants.ERROR_MESSAGE_NO_CONFIGURATION);
		}
		IContext mxContext = Core.createSystemContext();
		UserProvisioningHelper userProvisioningHelper = new UserProvisioningHelper();
		User mxUser = userProvisioningHelper.updateUser(mxContext,assertionAttributes, ssoConfig);
		String userName = mxUser.getName();
		IUser newUser = Core.getUser(mxContext, userName);
		SAMLSessionInfo samlSessionInfo = new SAMLSessionInfo(mxSAMLAssertion, entityId, config, newUser);
		samlSessionInfo.setDeleteLock();
		try {
			// custom login functionality
			if (config.useCustomUserProvisioning) {
				_logNode.trace(" Executing custom logic functionality for user p:'" + principalValue + "'/u:'" + userName + "'");

				UUID sessionId = UUID.randomUUID();
				while (this.activeSessions.containsKey(sessionId))
					sessionId = UUID.randomUUID();

				samlSessionInfo.setSessionId(sessionId);
				this.activeSessions.put(sessionId, samlSessionInfo);

				IMendixObject samlSessionObject = Core.instantiate(mxContext, SAMLSession.entityName);
				samlSessionObject.setValue(mxContext, SAMLSession.MemberNames.SessionID.toString(), sessionId.toString());
				samlSessionObject.setValue(mxContext, SAMLSession.MemberNames.Username.toString(), userName);

				samlSessionInfo.setSAMLSessionID(sessionId.toString());

				// BJHL 2016-12-05 removed retain/release, which was introduced as a fix in r90 on 2014-07-31

				try {
					userProvisioningHelper.executeCustomProvisioningMicroFlow(mxContext, config.customProvisioningMicroflow, samlSessionObject, mxUser, assertionAttributes, ssoConfig);
				} catch (SAMLFeedbackException fe) {
					throw fe;
				} catch (Exception e) {
					throw new SAMLException("Exception occured while executing the customLoginLogic Microflow, the error was: " + e.getMessage(), e);
				}
			}

			// Get the user again, with the latest values from the just executed microflow
			samlSessionInfo.setUserRecord(Core.getUser(mxContext, userName));

			if (ssoConfig.getEnableDelegatedAuthentication(mxContext)) {
				String delAuthURL = ssoConfig.getDelegatedAuthenticationURL(mxContext);
				if (delAuthURL != null && !"".equals(delAuthURL.trim()))
					SAMLRequestHandler.getInstance(mxContext).requestDelegatedAuthentication(samlSessionInfo.getSamlSessionID(), delAuthURL);
				else
					_logNode.error("Invalid SSO configuration(" + ssoConfig.getAlias() + ") - Delegated authentication is enabled, but no URL has been specified.");
			}
		} catch (Exception e) {
			samlSessionInfo.releaseLock();
			// BJHL 2016-12-05 removed retain/release, which was introduced as a fix in r90 on 2014-07-31
			throw e;
		}

		_logNode.trace(" Finished evaluating user with name: p:'" + principalValue + "'/u:'" + newUser.getName() + "'");
		return samlSessionInfo;
	}

	public SAMLSessionInfo isLoggedIn( ISession mxSession ) {
		if ( mxSession == null )
			return null;

		return this.activeSessions.get(mxSession.getId());
	}

	public void logOut( ISession session ) {
		if ( session == null )
			return;

		if ( destoySAMLSessionInfo(session.getId()) )
			this.activeSessions.remove(session.getId());

		Core.logout(session);
	}

	public boolean destoySAMLSessionInfo( UUID sessionId ) {

		if ( this.activeSessions.containsKey(sessionId) ) {
			return this.activeSessions.get(sessionId).isRemovalAllowed();
		}

		return false;
	}


	private void evaluateActiveSessions() {
		try {
			List<UUID> sessionsToDestoy = new ArrayList<UUID>();
			for( Entry<UUID, SAMLSessionInfo> entry : this.activeSessions.entrySet() ) {
				UUID sessionId = entry.getKey();

				if ( Core.getSessionById(sessionId) == null ) {
					if ( _logNode.isDebugEnabled() )
						_logNode.debug("SessionManager - Attempting to clean up session: " + sessionId.toString() + " since the Mx Session is no longer active");

					if ( destoySAMLSessionInfo(sessionId) )
						sessionsToDestoy.add(sessionId);
				}
			}

			if ( _logNode.isDebugEnabled() )
				_logNode.debug("SessionManager - Removed sessions: " + sessionsToDestoy.toString());
			for( UUID sessionID : sessionsToDestoy )
				this.activeSessions.remove(sessionID);
		}
		catch( Exception e ) {
			_logNode.error(e);
		}
	}

	public SAMLSessionInfo getSessionDetails( IMxRuntimeRequest request ) throws SAMLException {
		try {
			ISession session = getSessionFromRequest(request);
			if ( session != null ) {
				return this.activeSessions.get(session.getId());
			}

			return null;
		}
		catch( CoreException e ) {
			throw new SAMLException(e);
		}
	}

	public SAMLSessionInfo getSessionDetails( String sessionId ) {
		if ( sessionId == null )
			return null;

		UUID sessionUuid = UUID.fromString(sessionId);

		if ( _logNode.isTraceEnabled() ) {

			for( UUID ses : this.activeSessions.keySet() ) {
				_logNode.trace("Active Session: " + ses);
			}

		}
		return this.activeSessions.get(sessionUuid);
	}

	public ISession getSessionFromRequest( IMxRuntimeRequest request ) throws CoreException {
		String sessionId = getSamlSessionID(request);
		if ( sessionId == null )
			return null;

		UUID curUUID = UUID.fromString(sessionId);
		ISession session = Core.getSessionById(curUUID);

		if ( session == null || !session.isInteractive() )
			return null;

		return session;
	}

	// This method returns initially logged in IDP as part of In-Session Authentication
	public String getInitiallyLoggedInIdp(ISession currentSession)
	{
		if(this.activeSessions!=null && currentSession!=null) {
			return this.activeSessions.get(currentSession.getId())!=null ? this.activeSessions.get(currentSession.getId()).getEntityId():null;
		}
		return null;
	}

	private String getSamlSessionID(IMxRuntimeRequest request) throws CoreException {
		return request.getCookie(Core.getConfiguration().getSessionIdCookieName(), true);
	}


}