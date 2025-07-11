package saml20.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.systemwideinterfaces.core.IContext;
import mxmodelreflection.proxies.Microflows;
import org.opensaml.saml.common.SAMLException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import saml20.implementation.binding.BindingHandler;
import saml20.implementation.common.Constants;
import saml20.implementation.common.Constants.SAMLAction;
import saml20.implementation.common.HTTPUtils;
import saml20.implementation.common.SAMLUtil;
import saml20.implementation.metadata.IdpMetadata;
import saml20.implementation.metadata.IdpMetadata.Metadata;
import saml20.implementation.wrapper.MxSAMLAuthnRequest;
import saml20.proxies.SAMLRequest;
import saml20.proxies.SSOConfiguration;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LoginHandler extends SAMLHandler {
	private static final ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

	@Override
	public void handleRequest( SAMLRequestContext context ) throws SAMLException {
		printTraceInfo(context);
		String requestID = java.util.UUID.randomUUID().toString();
		String relayState = Constants.RELAYSTATE_SEPARATOR + requestID;
		boolean isInSessionLogin = false;
		boolean enableForceAuthentication = false;

		IMxRuntimeRequest request = context.getRequest();


		// Checks whether action parameter is verify and update the relay state accordingly
		String action = request.getParameter(Constants.SSO_ACTION_PARAMETER);
		if (action != null && action.toLowerCase().equals(Constants.SSO_ACTION_VERIFY)) {
			relayState += Constants.RELAYSTATE_SEPARATOR + Constants.RELAYSTATE_VERIFY;
			isInSessionLogin = true;
			_logNode.trace("Verify parameter found, relayState is now: " + relayState);
		} else {
			isInSessionLogin = false;
			relayState += Constants.RELAYSTATE_SEPARATOR + Constants.RELAYSTATE_LOGIN;
			_logNode.trace("No action parameter found, relayState is now: " + relayState);
		}

		
		// retreives the context parameter
		String contextParameter = request.getParameter(Constants.SSO_CONTEXT_PARAMETER);
		if (contextParameter != null) {
			contextParameter= Jsoup.clean(contextParameter,Safelist.none());
			_logNode.trace("Context parameter is: " + contextParameter);
		}

		String continuation = request.getParameter(Constants._getInstance().SSO_CONTINUATION_PARAMETER);
		if (continuation != null && !continuation.equals("")) {
				relayState += Constants.RELAYSTATE_SEPARATOR + continuation;
			_logNode.info("Continuation parameter found, relayState is now: " + relayState);
		}

		IdpMetadata idpMetadata = context.getIdpMetadata();
		if( idpMetadata == null )
			throw new SAMLException("SAML hasn't been correctly initialized. Please restart the SAML handler.");
		Metadata metadata = null;

		if(isInSessionLogin)
		{
			String inSessionAuthenticationIdp= context.getSessionManager().getInitiallyLoggedInIdp(context.getCurrentSession());
			if(inSessionAuthenticationIdp!=null)
			{
				metadata = idpMetadata.findSupportedEntity(inSessionAuthenticationIdp);
			}

			/*else {
				throw new SAMLException("There is no active SAML session to initiate In-Session Authentication.");
			}*/
		}
		if(metadata == null) {
			String samlIdp = request.getParameter(Constants._getInstance().DISCOVERY_ATTRIBUTE);
			if (samlIdp == null || "".equals(samlIdp)) {
				String[] args = HTTPUtils.extractResourceArguments(request);
				if (SAMLAction.login.toString().equals(args[0]) && args.length > 1)
					samlIdp = args[1];
			}

			if (samlIdp != null && !"".equals(samlIdp)) {
				metadata = idpMetadata.findSupportedEntity(samlIdp);
			}
		}
		if( metadata == null ) {
			//If we only have a single IdP active, and we allow to default to the first IdP
			if( idpMetadata.getAllMetaData().size() == 1 && Constants._getInstance().SSO_DEFAULT_FIRST_IDP ){
				_logNode.debug("No supported IdP discovered, using first from metadata");
				metadata = idpMetadata.getFirstMetadata();
			}
			else if ( Constants._getInstance().DISCOVERY_ALLOWED ) {

				_logNode.debug("Discovery profile is active, trying to determine the IdP");
				String url = Constants._getInstance().getSP_URI() + Constants._getInstance().SSO_PATH;

				if ( request.getHttpServletRequest().getQueryString() != null ) {
					url += "?" + request.getHttpServletRequest().getQueryString();
				}
				try {
					String query = "r=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
					if(isInSessionLogin){
						query += Constants.getSSOActionParameter();
					}
					if(contextParameter != null){
						query += Constants.getSSOContextParameter(contextParameter);
					}
					if(continuation != null){
						continuation= URLEncoder.encode(continuation, StandardCharsets.UTF_8);
						query += Constants.getContinuationParameter(continuation);
					}

					HTTPUtils.sendMetaRedirect(context.getResponse(), Constants._getInstance().getSP_URI() + Constants._getInstance().SSO_DISCOVERY_PATH, query);

					//Stop the evaluation since we are doing a redirect
					return;
				}
				catch( IOException e ) {
					throw new SAMLException("Unable to redirect to discovery path: " + Constants._getInstance().getSP_URI() + Constants._getInstance().SSO_DISCOVERY_PATH, e);
				}
			}
			
			else {
				_logNode.warn("Discovery is disabled, and no IdP specified in the URL. Therefore we don't serve the user with an IdP. Enable discovery or allow using the default IdP");
				
				try {
					HTTPUtils.sendMetaRedirect(context.getResponse(), Constants._getInstance().getSP_URI() + Constants._getInstance().DISCOVERY_LANDINGPAGE, null);
					
					//Stop the evaluation since we are doing a redirect
					return;
				}
				catch( IOException e ) {
					throw new SAMLException("Unable to redirect to the discovery landing page: " + Constants._getInstance().getSP_URI() + Constants._getInstance().DISCOVERY_LANDINGPAGE, e);
				}
			}
		}
		else {
			_logNode.debug("Discovered idp " + metadata.getEntityID());
		}
		SSOConfiguration configuration = SSOConfiguration.initialize(context.getIContext(), metadata.getSsoConfiguration());
		context.setSSOConfiguration(configuration);
		org.opensaml.saml.saml2.metadata.Endpoint signonLocation = metadata.findLoginEndpoint(Constants.getBindingURIs());
		if ( signonLocation == null ) {
			String msg = "Could not find a valid IdP SignOn location. Supported bindings: " + Constants.getBindingURIs() + ", available: " + metadata.getSingleSignonServices();
			_logNode.error(msg);
			throw new SAMLException(msg);
		}
		_logNode.debug("Signing on at " + signonLocation.getLocation());

		enableForceAuthentication = configuration.getEnableForceAuthentication() || isInSessionLogin;
		MxSAMLAuthnRequest authnRequest = MxSAMLAuthnRequest.buildAuthnRequestObject(context, metadata, signonLocation, relayState, enableForceAuthentication, isInSessionLogin);

		// BJHL 2015-10-22 log the request after signing
		SAMLRequest samlRequest = SAMLUtil.logSAMLRequestMessage(context, requestID, authnRequest, metadata.getSsoConfiguration());

		// execute custom preflight microflow
		IContext mxContext = Core.createSystemContext();
		String customPrepareInSessionAuthNMicroflow = null;
		try {
			Microflows prepareInSessionAuthNMicroflow = configuration.getSSOConfiguration_CustomPrepareInSessionAuthenticationMicroflow();
			if (prepareInSessionAuthNMicroflow != null)
				customPrepareInSessionAuthNMicroflow = prepareInSessionAuthNMicroflow.getCompleteName();
			else
				customPrepareInSessionAuthNMicroflow = null;
		}catch( CoreException e ) {
			throw new SAMLException(e);
		}

		if (customPrepareInSessionAuthNMicroflow != null && !customPrepareInSessionAuthNMicroflow .equals("")) {
			Core.microflowCall(customPrepareInSessionAuthNMicroflow )
					.inTransaction(true)
					.withParam("SamlRequest", samlRequest.getMendixObject())
					.withParam("ContextOn", contextParameter)
					.execute(mxContext);
		}

		BindingHandler bindingHandler = context.getBindingHandlerFactory().getBindingHandler(signonLocation.getBinding());
		bindingHandler.handle(context.getRequest(), context.getResponse(), context, metadata, signonLocation, authnRequest, relayState);

		SAMLUtil.updateSAMLRequestMessage(samlRequest,context,  authnRequest, metadata.getSsoConfiguration());
	}

}
