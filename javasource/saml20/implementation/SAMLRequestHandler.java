package saml20.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.externalinterface.connector.RequestHandler;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.ISession;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.config.impl.GlobalSecurityConfigurationInitializer;
import saml20.implementation.binding.BindingHandlerFactory;
import saml20.implementation.common.Constants;
import saml20.implementation.common.Constants.SAMLAction;
import saml20.implementation.common.HTTPUtils;
import saml20.implementation.common.SAMLUtil;
import saml20.implementation.metadata.IdpMetadata;
import saml20.implementation.metadata.SPMetadata;
import saml20.implementation.security.CredentialRepository;
import saml20.implementation.security.SAMLSessionInfo;
import saml20.implementation.security.SessionManager;
import saml20.implementation.wrapper.MxResource;
import saml20.proxies.SSOConfiguration;

import javax.servlet.http.HttpServletResponse;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SAMLRequestHandler extends RequestHandler {

	public static ILogNode _logNode = Core.getLogger(Constants.LOGNODE);
	private transient IdpMetadata idpMetadata;
	private transient SPMetadata spMetadata;
	private BindingHandlerFactory bindingHandlerFactory;
	private transient SessionManager sessionManager;
	private transient VelocityEngine engine;

	private final Map<SAMLAction, SAMLHandler> handlers = new HashMap<SAMLAction, SAMLHandler>();
	private boolean initialized = false;
	private boolean requestHandlerRegistered = false;

	private static SAMLRequestHandler _instance = null;

	public static SAMLRequestHandler getInstance(IContext context) throws InitializationException {
		if (_instance == null)
			_instance = new SAMLRequestHandler(context);

		return _instance;
	}

	private SAMLRequestHandler(IContext context) throws InitializationException {
//        org.opensaml.DefaultBootstrap.bootstrap();
		InitializationService.initialize(); // TODO: JEROEN / verify if this is correct
		try {
			initServlet(context, false);
		} catch (Exception e) {
			_logNode.error("Unable to initialize the Servlet configuration", e);
		}

		this.engine = HTTPUtils.getEngine();
	}

	public void initServlet(IContext context, boolean forceReload) throws SAMLException, InitializationException {
		if (!this.initialized || forceReload) {

			List<IMendixObject> ssoConfigurationList = SAMLUtil.getActiveSSOConfig(context);
			IMendixObject spMetadataConfiguration = SAMLUtil.getMetadataConfig(context);

			this.handlers.clear();

			// Make sure the SP url is properly set in the configuration entity
//			if ( this.ssoConfigurationList != null ) {

			this.handlers.putAll(Constants.getHandlers());
			if (_logNode.isDebugEnabled())
				_logNode.debug("Found handlers: " + this.handlers);

			String entityId = spMetadataConfiguration.getValue(context, saml20.proxies.SPMetadata.MemberNames.EntityID.toString());
			if (entityId == null || entityId.isEmpty())
				throw new SAMLFeedbackException("There was no entity Id specified in the SP Metadata, please configure the Entity ID before using SSO.").addFeedbackMessage(Constants.ERROR_MESSAGE_NO_CONFIGURATION);


			this.bindingHandlerFactory = new BindingHandlerFactory();
			CredentialRepository credentialRepository = CredentialRepository.getInstance();
			if (ssoConfigurationList != null) {
				this.idpMetadata = IdpMetadata.getInstance().updateConfiguration(context, ssoConfigurationList);
				credentialRepository.updateConfiguration(context, ssoConfigurationList,this.idpMetadata);
			}else{
				this.idpMetadata = null;
			}

			//this.credential = credentialRepository.getCredential(Constants.CERTIFICATE_PASSWORD, entityId);

			this.sessionManager = SessionManager.getInstance(context).init(context, ssoConfigurationList);
			this.spMetadata = SPMetadata.getInstance().updateConfiguration(context, spMetadataConfiguration, credentialRepository);

			//initializes the various security configurations
			GlobalSecurityConfigurationInitializer gci = new GlobalSecurityConfigurationInitializer();
			gci.init();

			this.initialized = true;

			if (!this.requestHandlerRegistered) {
				Core.addRequestHandler(Constants._getInstance().SSO_PATH, this);
				Core.addRequestHandler(Constants._getInstance().SSO_PATH.toLowerCase(), this);

				Core.addRequestHandler(Constants._getInstance().SSO_LOGOUT_PATH, this);
				Core.addRequestHandler(Constants._getInstance().SSO_LOGOUT_PATH.toLowerCase(), this);

				Core.getLogger(Constants.LOGNODE).info("SAML SSO RequestHandler has been added to path '" + Constants._getInstance().SSO_PATH + "'");
				this.requestHandlerRegistered = true;
			}
		}
	}

	@Override
	public void processRequest(IMxRuntimeRequest request, IMxRuntimeResponse response, String arguments) {
		try {
			IContext context = Core.createSystemContext();
			initServlet(context, false);

			String[] resourceArgs = HTTPUtils.extractResourceArguments(request);

			//resourceArgs[0] = the action, decide on the default action
			if ("".equals(resourceArgs[0])) {
				if (request.getParameter(Constants.SAML_SAMLRESPONSE) == null)
					resourceArgs[0] = SAMLAction.login.toString();                //No SAML response let's assume we have a login

				else if (request.getParameter(Constants.SAML_SAMLRESPONSE) != null &&
						!"".equals(request.getParameter(Constants.SAML_SAMLRESPONSE)))
					resourceArgs[0] = SAMLAction.assertion.toString();                //We can find a SAML response, must be an assertion request
			}

			SAMLAction action = getAction(resourceArgs[0]);
			_logNode.debug("Start processing action (" + action + "/" + resourceArgs[0] + ") " + (request.getParameter(Constants.SAML_SAMLRESPONSE) == null ? "without SAMLResponse" : "with SAMLResponse"));

			if (this.handlers.containsKey(action)) {
				try {
					SAMLRequestContext samlContext = new SAMLRequestContext(context, request, response, this.idpMetadata, this.spMetadata,this.sessionManager, this.bindingHandlerFactory, this.engine, this.getSessionFromRequest(request));

					ISession session = samlContext.getSessionManager().getSessionFromRequest(request);
					SAMLSessionInfo sessionInfo = samlContext.getSessionManager().isLoggedIn(session);
					updateSSOConfigurationInSAMLRequestContext(sessionInfo,samlContext);

					SAMLHandler handler = this.handlers.get(action);
					handler.handleRequest(samlContext);
				} catch (Exception e) {
					handleError(response, e);
				}
			} else {
				_logNode.debug("Unsupported action: [" + resourceArgs[0] + "] was requested, only " + this.handlers.keySet() + " are supported.");
				throw new SAMLFeedbackException("Unsupported action was requested, only " + this.handlers.keySet() + " are supported.");
			}
		} catch (Exception e) {
			handleError(response, e);
		}
	}

	private  SAMLAction getAction(String action) throws SAMLFeedbackException {
		try{
			return SAMLAction.valueOf(action);
		}catch (Exception e) {
			_logNode.error("Unsupported action: [" + action + "] was requested, only " + this.handlers.keySet() + " are supported.");
			throw new SAMLFeedbackException("Oops! The End-point you’re looking for doesn’t exist.").setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void updateSSOConfigurationInSAMLRequestContext(SAMLSessionInfo sessionInfo, SAMLRequestContext samlContext)  {
		if (sessionInfo != null && sessionInfo.getEntityId() != null && samlContext.getIdpMetadata() != null) {
			try {
				IdpMetadata.Metadata metadata = samlContext.getIdpMetadata().getMetadata(sessionInfo.getEntityId());
				SSOConfiguration configuration = SSOConfiguration.initialize(samlContext.getIContext(), metadata.getSsoConfiguration());
				samlContext.setSSOConfiguration(configuration);
			} catch (SAMLException e) {
				_logNode.error("Error occurred while getting SSO: " + e.getMessage());
			}
		}
	}

	public void requestDelegatedAuthentication(String samlSessionID, String resourceURL) throws SAMLException, InitializationException {

		IContext context = Core.createSystemContext();
		initServlet(context, false);

		SAMLRequestContext samlContext = new SAMLRequestContext(context, null, null, this.idpMetadata, this.spMetadata,this.sessionManager, this.bindingHandlerFactory, this.engine, null);
		samlContext.setSamlSessionID(samlSessionID);
		samlContext.setResource(new MxResource(resourceURL));

		SAMLSessionInfo sessionInfo = samlContext.getSessionManager().getSessionDetails(samlSessionID);
		updateSSOConfigurationInSAMLRequestContext(sessionInfo,samlContext);

		SAMLHandler handler = this.handlers.get(SAMLAction.delegatedAuthentication);
		handler.handleRequest(samlContext);

	}

	private void handleError(IMxRuntimeResponse response, Exception e) {
		String DEFAULT_MESSAGE = "Unable to validate the SAML message!";

		_logNode.error("Error occurred while making request: " + e.getMessage());
		_logNode.debug("Unable to validate Response, see SAMLRequest overview for detailed response.", e);

		VelocityContext ctx = new VelocityContext();
         int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		 if (e instanceof SAMLFeedbackException) {
			ctx.put(Constants.ATTRIBUTE_ERROR, StringEscapeUtils.escapeHtml(((SAMLFeedbackException) e).getFeedbackMessage()));
			ctx.put(Constants.ATTRIBUTE_EXCEPTION, null);
			 status = ((SAMLFeedbackException) e).getStatus();
		} else {
			ctx.put(Constants.ATTRIBUTE_ERROR, StringEscapeUtils.escapeHtml(DEFAULT_MESSAGE));
			ctx.put(Constants.ATTRIBUTE_EXCEPTION, null);
		}

		if (Constants._getInstance().getLoginPage() != null && !Constants._getInstance().getLoginPage().isEmpty())
			ctx.put(Constants.ATTRIBUTE_APPLICATION_LOCATION, Constants._getInstance().getSP_URI() + Constants._getInstance().getLoginPage());

		ctx.put(Constants.ATTRIBUTE_APPLICATION_SSO_LOCATION, Constants._getInstance().getSP_URI() + Constants._getInstance().SSO_PATH);

		response.setContentType("text/html");
		response.setStatus(status);

		try {
			Writer writer = response.getWriter();
			this.engine.mergeTemplate("templates/saml2-error-result.vm", "UTF-8", ctx, writer);
			writer.flush();
		} catch (Exception e1) {
			_logNode.error("Unable to render error template", e1);
		}
	}
}
