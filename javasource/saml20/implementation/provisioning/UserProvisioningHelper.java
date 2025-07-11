package saml20.implementation.provisioning;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import mxmodelreflection.proxies.MxObjectMember;
import saml20.implementation.SAMLFeedbackException;
import saml20.implementation.common.Constants;
import saml20.implementation.common.SAMLUtil;
import saml20.proxies.*;
import system.proxies.User;
import usercommons.proxies.ClaimEntityAttribute;
import usercommons.proxies.UserClaim;
import usercommons.proxies.UserInfoParam;

import java.util.*;
import java.util.stream.Collectors;

public class UserProvisioningHelper {
    private static ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

    public static final String UseNameID = "UseNameID";

    /**
     * return principal assertion attribute value
     * @param mxContext
     * @param assertionAttributes
     * @param ssoConfig
     * @return
     * @throws SAMLFeedbackException
     */
    public String getPrincipalValue(IContext mxContext,HashMap<String, Object> assertionAttributes, SSOConfiguration ssoConfig) throws SAMLFeedbackException {
        String principalKey = this.getPrincipalKey(mxContext, ssoConfig);

        if (principalKey != null) {
            _logNode.info("Assertion attributes " + assertionAttributes.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining("\n")));
            if (assertionAttributes.containsKey(principalKey)) {
                Object pValue = assertionAttributes.get(principalKey);
                if (pValue instanceof String) {
                    //BJHL 20170123 removed the "uselowercase" constant again, since we are now in mx7 where this is the default
                    return ((String) pValue).toLowerCase();
                }
            } else {
                String errorMessage = "The selected principal key " + principalKey + " in the configuration, is not available in the response";
                SAMLUtil.createLogLine(errorMessage, SSOLogResult.Failed);
                throw new SAMLFeedbackException("The principal key selected in the SSO configuration is not available in the response.").addFeedbackMessage(Constants.ERROR_MESSAGE_NO_CONFIGURATION);
            }
        } else {
            String errorMessage = "There is no principal key selected in the SSO Configuration. Please review the configuration.";
            SAMLUtil.createLogLine(errorMessage, SSOLogResult.Failed);
            throw new SAMLFeedbackException(errorMessage).addFeedbackMessage(Constants.ERROR_MESSAGE_NO_CONFIGURATION);
        }

        return null;
    }

    /**
     * return principal assertion attribute name
     * @param mxContext
     * @param ssoConfig
     * @return
     * @throws SAMLFeedbackException
     */
    private String getPrincipalKey(IContext mxContext,SSOConfiguration ssoConfig) throws SAMLFeedbackException {
        try {
            usercommons.proxies.UserProvisioning userProvisioning = ssoConfig.getSSOConfiguration_UserProvisioning();
            String mxObjectMember = userProvisioning.getCustomEntityMember();
            if (mxObjectMember != null) {
                List<IMendixObject> claimAttributeMapping = Core.retrieveByPath(mxContext, userProvisioning.getMendixObject(), "UserCommons.ClaimEntityAttribute_UserProvisioning");
                if (claimAttributeMapping != null && !claimAttributeMapping.isEmpty()) {
                    String entityPrimeAttribute = mxObjectMember;
                    for (IMendixObject entityAttributesListElement : claimAttributeMapping) {
                        ClaimEntityAttribute claimEntityAttribute = usercommons.proxies.ClaimEntityAttribute.initialize(mxContext, entityAttributesListElement);
                        if (claimEntityAttribute != null && entityPrimeAttribute.equals(claimEntityAttribute.getEntityMemberName())) {
                            return claimEntityAttribute.getClaimEntityAttribute_Claim().getName();
                        }
                    }
                }
            }
            return null;
        }catch (Exception e) {
            String errorMessage = "Unable to retrieve a principal key from SSO configuration, because of error: " + e.getMessage();
            SAMLUtil.createLogLine(errorMessage, SSOLogResult.Failed);
            throw new SAMLFeedbackException("Unable to retrieve a principal key from SSO configuration.").addFeedbackMessage(Constants.ERROR_MESSAGE_NO_CONFIGURATION);
        }
    }

    /**
     * Create or update user with assertion attributes
     * @param mxContext
     * @param assertionAttributes
     * @param ssoConfig
     * @return User
     * @throws Exception
     */
    public User updateUser(IContext mxContext, HashMap<String, Object> assertionAttributes, SSOConfiguration ssoConfig) throws Exception {
        UserInfoParam userInfoParam = createUserInfoParam(assertionAttributes, ssoConfig, mxContext);
        return usercommons.proxies.microflows.Microflows.createOrUpdateUser(mxContext, userInfoParam);
    }

    /**
     * Execute Custom Provisioning MicroFlow in SAML configuration
     * @param mxContext
     * @param customProvisioningMicroflow
     * @param samlSessionObject
     * @param mxUser
     * @param assertionAttributes
     * @param ssoConfig
     * @throws SAMLFeedbackException
     */
    public void executeCustomProvisioningMicroFlow(IContext mxContext, String customProvisioningMicroflow, IMendixObject samlSessionObject, User mxUser, HashMap<String, Object> assertionAttributes, SSOConfiguration ssoConfig) throws SAMLFeedbackException {
        ArrayList<AssertionAttribute> astAttrList = createAssertionAttributeList(mxContext, assertionAttributes);
        LoginFeedback feedback = null;
        if (customProvisioningMicroflow != null && !customProvisioningMicroflow.equals("")) {
            List<IMendixObject> astAttributes = astAttrList.stream().map(attr -> attr.getMendixObject()).collect(Collectors.toList());
            IMendixObject feedbackObj = Core.microflowCall(customProvisioningMicroflow)
                    .inTransaction(true)
                    .withParam("SSOConfiguration", ssoConfig.getMendixObject())
                    .withParam("AssertionAttributeList", astAttributes)
                    .withParam("SAMLSession", SAMLSession.initialize(mxContext, samlSessionObject).getMendixObject())
                    .withParam("User", mxUser.getMendixObject())
                    .execute(mxContext);
            feedback = feedbackObj != null ? LoginFeedback.initialize(mxContext, feedbackObj) : null;
        } else {
            feedback = saml20.proxies.microflows.Microflows.customUserProvisioning(mxContext, SAMLSession.initialize(mxContext, samlSessionObject),
                    mxUser, ssoConfig, astAttrList);
        }

        if (feedback != null) {

            if (!feedback.getLoginAllowed(mxContext)) {
                _logNode.info("Login aborted, CustomLoginLogic instructed that login is not allowed for user: '" + mxUser.getName() + "'");
                String feedbackMsg = feedback.getFeedbackMessageHTML(mxContext);
                if (feedbackMsg != null && !"".equals(feedbackMsg.trim())) {
                    throw new SAMLFeedbackException(feedbackMsg);
                } else
                    throw new SAMLFeedbackException("The authentication was successful, but your account could not be setup in this application with the provided information.");
            }
        }
    }

    /**
     * create UserInfoParam object contains assertion attributes and UserProvisioning
     * @param assertionAttributes
     * @param ssoConfig
     * @param mxContext
     * @return
     * @throws CoreException
     */
    private UserInfoParam createUserInfoParam(HashMap<String, Object> assertionAttributes, SSOConfiguration ssoConfig, IContext mxContext) throws CoreException {
        IMendixObject mxUserInfo = Core.instantiate(mxContext, UserInfoParam.getType());
        UserInfoParam userInfoParam = UserInfoParam.initialize(mxContext, mxUserInfo);

        usercommons.proxies.UserProvisioning UserProvisioning = ssoConfig.getSSOConfiguration_UserProvisioning();
        userInfoParam.setUserInfoParam_UserProvisioning(UserProvisioning);

        List<UserClaim> userClaimList = createUserClaims(assertionAttributes, mxContext);
        userInfoParam.setUserClaim_UserInfoParam(userClaimList);

        return userInfoParam;
    }

    /**
     * create list of UserClaims from assertion attributes
     * @param assertionAttributes
     * @param mxContext
     * @return  list UserClaim values
     */
    private List<UserClaim> createUserClaims(HashMap<String, Object> assertionAttributes, IContext mxContext) {
        Set<String> claims = assertionAttributes.keySet();
        List<UserClaim> userClaimList = new ArrayList<>();
        for (String claim : claims) {
            Object value = assertionAttributes.get(claim);
            UserClaim userClaim = new UserClaim(mxContext);
            userClaim.setName(claim);
            userClaimList.add(userClaim);
            if ( value instanceof String[] ) {
                String listOfValues = new String();
                for( String s : (String[]) value ) {
                    if ( !listOfValues.isEmpty() ) {
                        listOfValues += ";";
                    }
                    listOfValues += s;
                    UserClaimValue userClaimValue = new UserClaimValue(mxContext);
                    userClaimValue.setValue(s);
                    userClaimValue.setUserClaimValue_UserClaim(userClaim);
                }
                value = listOfValues;
            }
            userClaim.setValue((String) value);
        }
        return userClaimList;
    }

    /**
     * Evaluate the list of assertions and build the list to pass into a microflow
     *
     *
     * @param mxContext
     * @param assertionAttributes
     * @return AssertionAttribute list
     */
    private ArrayList<AssertionAttribute> createAssertionAttributeList(IContext mxContext, HashMap<String, Object> assertionAttributes) {
        ArrayList<AssertionAttribute> currentList = new ArrayList<>(assertionAttributes.size());
            for (Map.Entry<String, Object> assertionAttributeEntry : assertionAttributes.entrySet()) {
                Object value = assertionAttributeEntry.getValue();
                if (value instanceof String) {
                    AssertionAttribute astAttr = new AssertionAttribute(mxContext);
                    astAttr.setKey(assertionAttributeEntry.getKey());
                    astAttr.setValue((String) value);
                    currentList.add(astAttr);
                } else if (value instanceof String[]) {
                    for (String iStr : (String[]) value) {
                        AssertionAttribute astAttr = new AssertionAttribute(mxContext);
                        astAttr.setKey(assertionAttributeEntry.getKey());
                        astAttr.setValue(iStr);
                        currentList.add(astAttr);
                    }
                } else
                    _logNode.error("Unexpected value " + value + " for key: " + assertionAttributeEntry.getKey());
            }
        return currentList;
    }
}
