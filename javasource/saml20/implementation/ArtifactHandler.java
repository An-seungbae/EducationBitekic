package saml20.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.security.credential.Credential;
import saml20.implementation.common.Constants;
import saml20.implementation.common.SAMLUtil;
import saml20.implementation.delegation.ArtifactResolver;
import saml20.implementation.metadata.IdpMetadata.Metadata;
import saml20.implementation.provisioning.UserProvisioningHelper;
import saml20.implementation.security.SAMLSessionInfo;
import saml20.implementation.wrapper.MxSAMLAssertion;
import saml20.implementation.wrapper.MxSAMLResponse;
import saml20.proxies.*;

import java.util.HashMap;
import java.util.List;

public class ArtifactHandler extends SAMLHandler {
    private static final ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

    @Override
    public void handleRequest(SAMLRequestContext context) throws SAMLException {
        printTraceInfo(context);
        MxSAMLResponse response = ArtifactResolver.getResponse(context);
        handleSAMLResponse(context, response);
    }

    private void handleSAMLResponse(SAMLRequestContext samlContext, MxSAMLResponse response) throws SAMLException {
        SAMLRequest correspondingSAMLRequest = null;
        SSOConfiguration ssoConfig = samlContext.getSSOConfiguration();
        String userPrincipal = null;

        try {
            IMxRuntimeRequest request = samlContext.getRequest();

            String relayState = request.getParameter(Constants.SAML_RELAYSTATE);
            if ( _logNode.isDebugEnabled() )
                _logNode.debug("RelayState..:" + relayState);

            String requestID = SAMLUtil.getRequestIDFromRelayState(relayState);
            correspondingSAMLRequest = SAMLUtil.retrieveCorrespondingRequestFromDB(samlContext.getIContext(), requestID);

            String entityId = sanitizeInput(response.getOriginalIdpEntityId());
            Metadata metadata = samlContext.getIdpMetadata().getMetadata(entityId);
            String entityAlias = sanitizeInput(metadata.getAlias(samlContext.getIContext()));

            //SAMLRequest has already received a response.
            if (correspondingSAMLRequest != null && YesNo.Yes.equals(correspondingSAMLRequest.gethasResponse())) {
                throw new SAMLFeedbackException("Request has already received a response").addFeedbackMessage("Unable to complete the request");
            } else if(correspondingSAMLRequest == null && SAMLUtil.hasAlreadyResponseProcessed(samlContext.getIContext(),response.getResponse().getID())){
                throw new SAMLFeedbackException("Request has already received a response").addFeedbackMessage("Unable to complete the request");
            }
            String spAssertionConsumerURL = samlContext.getSpMetadata().getAssertionConsumerServiceLocation(0);
            response.validateResponseCodeAndDestination(spAssertionConsumerURL);
            // getAssertion() decrypts assertions and adds the decrypted form to the message, invalidating the signature.
            // We check the message signature first to prevent this problem
            if (response.hasSignature()) {
             if(!response.validateMessageSignature(metadata)){
                 throw new SAMLException("The response is not signed correctly" + " ID:[" + response.getResponse().getID() + "]");
             }
            }

            // get the assertion node, this also decrypts if encrypted
            MxSAMLAssertion assertion = response.getAssertion(samlContext.getCredential());

            //  if assertions are signed, check the signature
            if (assertion.hasSignature()) {
                if(!response.validateAssertionSignature(assertion, metadata)){
                    throw new SAMLException("The assertion is not signed correctly");
                }
            }
            response.getResponse().getAssertions().add(assertion.getAssertion());
            String spEntityID = samlContext.getSpMetadata().getEntityID();
            //String spAssertionConsumerURL = samlContext.getSpMetadata().getAssertionConsumerServiceLocation(0);
            
            // run the other validations
            response.validateAssertionEmptyAndResponseToIsSame(requestID !=null? Constants.RELAYSTATE_SEPARATOR + requestID:null);
            assertion.validateAssertion(spEntityID, spAssertionConsumerURL);

            
            // check if at least something was signed
            if (ssoConfig.getUseEncryption() || Enum_ProtocolBinding.POST_BINDING.equals(ssoConfig.getResponseProtocolBinding()))  {
                if (!response.hasSignature() && !assertion.hasSignature()) {
                    throw new SAMLException("The message is not signed correctly, either the full message or the assertions must be signed");
                }
            }

            /*
             * The corresponding SAMLRequest is retrieved based on the RelayState. According to the SAML spec, the IdP
             * is required to answer all requests with an identical RelayState value as in the original request.
             *
             * We do however allow to override this, if it is configured in the SSOConfig we allow the IdP to start by
             * sending an Artifact
             */
            if (correspondingSAMLRequest == null && !ssoConfig.getAllowIdpInitiatedAuthentication()) {
                String errorMessage = "No request found with ID '" + requestID + "'";
                SAMLUtil.createLogLine(errorMessage, SSOLogResult.Failed);
                throw new SAMLFeedbackException("Nothing was returned for the requested ID.").addFeedbackMessage("Unable to complete the request");
            } else {
                try {
                    samlContext.getIContext().startTransaction();

                    HashMap<String, Object> assertionAttributes = null;
                    try {
                        assertionAttributes = retrieveAssertionAttributes(assertion,samlContext.getCredential());
                        assertionAttributes.put(UserProvisioningHelper.UseNameID,SAMLUtil.getNameIDValue(assertion));
                    } catch (Exception e2) {
                        throw new SAMLException("Unable to retrieve the assertion attributes, " + e2.getMessage(), e2);
                    }
                    userPrincipal = new UserProvisioningHelper().getPrincipalValue(samlContext.getIContext(),assertionAttributes,ssoConfig);
                    if (userPrincipal == null) {
                        // should not happen since either user principal is set, or one of many other exceptions has been thrown.
                        String errorMessage = "No user principal found.";
                        SAMLUtil.createLogLine(errorMessage, SSOLogResult.Failed);
                        throw new SAMLFeedbackException("No user principal found.");
                    }

                    // now we create a session
                    try {
                        SAMLSessionInfo samlSession = samlContext.getSessionManager().createSAMLSessionInfo(entityId, userPrincipal, assertionAttributes, ssoConfig, assertion);
                        if (samlSession != null) {
                            if (samlSession.getIUser().isActive()) {

                                if (samlSession.getIUser().getUserRoleNames().isEmpty()) {
                                    String errorMessage = "No user roles found for the provided user '" + samlSession.getIUser().getName() + "'";
                                    SAMLUtil.createLogLine(errorMessage, SSOLogResult.Failed);
                                    throw new SAMLFeedbackException("No user roles found for the provided user.").addFeedbackMessage("Your account has not been configured to access this application.");
                                } else {
                                    String action = SAMLUtil.getActionFromRelayState(relayState);
                                    if (action != null && action.equals(Constants.RELAYSTATE_VERIFY)) {
                                        // if verify action, then do not sign in user but execute custom verify microflow
                                        samlContext.getSessionManager().executeVerifyMicroflow(entityId, samlContext, samlSession, correspondingSAMLRequest, entityAlias, relayState);
                                        SAMLUtil.createLogLine("Successful verification on: " + samlSession.getIUser().getName(), SSOLogResult.Success);
                                    } else {
                                        // default behaviour is to sign in the user
                                        samlContext.getSessionManager().createSession(entityId, samlContext, samlSession, correspondingSAMLRequest, entityAlias, relayState);
                                        SAMLUtil.createLogLine("Successful sign on: " + samlSession.getIUser().getName(), SSOLogResult.Success);
                                    }
                                }
                            } else {
                                SAMLUtil.createLogLine("Inactive account: '" + samlSession.getIUser().getName() + "'", SSOLogResult.Failed);
                                throw new SAMLFeedbackException("Your account is not active.");
                            }
                        } else {
                            String errorMessage = "User lookup of '" + userPrincipal + "' failed, this user principal does not exist in the Mx database.";
                            SAMLUtil.createLogLine(errorMessage, SSOLogResult.Failed);
                            throw new SAMLFeedbackException("User lookup failed.").addFeedbackMessage("The authentication was successful, but there is no account available in this application.");
                        }

                    } catch (SAMLFeedbackException e) {
                        throw e;
                    } catch (Exception e) {
                        String errorMessage = "Could not create a session for the provided user principal '" + userPrincipal + "': " + e.getMessage();
                        SAMLUtil.createLogLine(errorMessage, SSOLogResult.Failed);
                        throw new SAMLFeedbackException("Could not create a session for the provided user principal.", e).addFeedbackMessage("An unexpected error occured while creating a session");
                    }

                    samlContext.getIContext().endTransaction();
                } catch (Exception e) {
                    if (samlContext.getIContext().isInTransaction())
                        samlContext.getIContext().rollbackTransaction();

                    throw e;
                }
            }
        } finally {
            SAMLUtil.logSAMLResponseMessage(samlContext, correspondingSAMLRequest, response.getResponse(), userPrincipal, ssoConfig.getMendixObject());
        }
    }

    private String sanitizeInput(String relayState) {
        if (relayState == null) {
            return null;
        }
        return relayState.replaceAll("[\r\n]", "").trim();
    }

    public HashMap<String, Object> retrieveAssertionAttributes(MxSAMLAssertion mxSAMLAssertion, Credential credential) throws CoreException {
        HashMap<String, Object> hashmap = new HashMap<String, Object>();

        List<AttributeStatement> attributeStatements = mxSAMLAssertion.getAssertion().getAttributeStatements();

        for (int i = 0; i < attributeStatements.size(); i++) {
            List<Attribute> attributes = attributeStatements.get(i).getAttributes();
            // TODO it is also possible to only encrypt attributes and not the full assertion
            // element. Do we want to support encrypted attributes?
            // attributeStatements.get(0).getEncryptedAttributes();


            for (int x = 0; x < attributes.size(); x++) {
                String strAttributeName = attributes.get(x).getDOM().getAttribute("Name");

                List<XMLObject> attributeValues = attributes.get(x).getAttributeValues();

                for (int y = 0; y < attributeValues.size(); y++) {
                    String strAttributeValue = attributeValues.get(y).getDOM().getTextContent();

                    // Replace attribute name key value pair, when value contains EncryptedID element with the NameID
                    NameID nameID = OpenSAMLUtils.getNameIdFromEncryptedID(attributeValues.get(y), credential);
                    if (nameID != null) {
                        strAttributeName = nameID.getNameQualifier();
                        strAttributeValue = nameID.getValue();
                        _logNode.info("getNameIdFromEncryptedID= " + strAttributeName + " : " + strAttributeValue);
                    }

                    if (hashmap.containsKey(strAttributeName)) {
                        Object value = hashmap.get(strAttributeName);
                        String[] valueArr = null;
                        if (value instanceof String) {
                            valueArr = new String[2];
                            valueArr[0] = (String) value;
                            valueArr[1] = (String) strAttributeValue;

                            hashmap.put(strAttributeName, valueArr);
                        } else if (value instanceof String[]) {
                            valueArr = new String[((String[]) value).length + 1];
                            for (int j = 0; j < ((String[]) value).length; j++)
                                valueArr[j] = ((String[]) value)[j];

                            valueArr[((String[]) value).length] = (String) strAttributeValue;

                            hashmap.put(strAttributeName, valueArr);
                        } else {
                            _logNode.error("Unexpected value " + value + " for key: " + strAttributeName);
                        }
                    } else {
                        hashmap.put(strAttributeName, strAttributeValue);
                    }
                }
            }
        }

        return hashmap;
    }
}
