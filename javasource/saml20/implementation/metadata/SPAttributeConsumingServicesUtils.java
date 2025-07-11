package saml20.implementation.metadata;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import saml20.implementation.common.Constants;
import saml20.implementation.common.MendixUtils;
import saml20.proxies.Enum_Attribute_Consuming_Login_Type;
import saml20.proxies.SPAttributeConsumingService;
import saml20.proxies.SPRequestedAttribute;
import saml20.proxies.SSOConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SPAttributeConsumingServicesUtils {
    private static final ILogNode _logNode = Core.getLogger(Constants.LOGNODE);
    public static List<SPAttributeConsumingService> retrieveCorrespondingSPAttributeConsumingService(IContext context, SSOConfiguration ssoConfiguration) {
        if (ssoConfiguration == null ) {
            return Collections.emptyList();
        }
        try {
            @SuppressWarnings("serial")
            List<IMendixObject> spAttributeConsumingServices = MendixUtils.retrieveFromDatabase(context, "//%s[%s = $ssoConfiguration][%s = $isActive]",
                    new HashMap<String, Object>() {{
                        put("ssoConfiguration", ssoConfiguration.getMendixObject().getId());
                        put("isActive", Boolean.TRUE);
                    }},
                    SPAttributeConsumingService.entityName,
                    SPAttributeConsumingService.MemberNames.SPAttributeConsumingService_SSOConfiguration.toString(),
                    SPAttributeConsumingService.MemberNames.isActive.toString()
            );

            List<SPAttributeConsumingService> result = new java.util.ArrayList<>(spAttributeConsumingServices.size());
            for (IMendixObject obj : spAttributeConsumingServices)
                result.add(SPAttributeConsumingService.initialize(context, obj));
            return result;

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<SPRequestedAttribute> retrieveCorrespondingSPAttribute(IContext context, SPAttributeConsumingService spAttributeConsumingService) {
        if (spAttributeConsumingService == null ) {
            return Collections.emptyList();
        }
        try {
            @SuppressWarnings("serial")
            List<IMendixObject> spAttributes = MendixUtils.retrieveFromDatabase(context, "//%s[%s = $spAttributeConsumingService]",
                    new HashMap<String, Object>() {{
                        put("spAttributeConsumingService", spAttributeConsumingService.getMendixObject().getId());
                    }},
                    SPRequestedAttribute.entityName,
                    SPRequestedAttribute.MemberNames.SPRequestedAttribute_SPAttributeConsumingService.toString()
            );

            List<SPRequestedAttribute> result = new java.util.ArrayList(spAttributes.size());
            for (IMendixObject obj : spAttributes)
                result.add(SPRequestedAttribute.initialize(context, obj));
            return result;

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static Integer getAttributeConsumingServiceIndex(IContext context, SSOConfiguration ssoConfiguration, Enum_Attribute_Consuming_Login_Type loginType){
        if (ssoConfiguration == null ) {
            return -1;
        }
        Integer index = Integer.valueOf(-1);
        try {
            @SuppressWarnings("serial")
            List<IMendixObject> spAttributeConsumingServices = MendixUtils.retrieveFromDatabase(context,
                    "//%s[%s = $ssoConfiguration][%s = $isActive][%s = $loginType]",
                    new HashMap<String, Object>() {{
                        put("ssoConfiguration", ssoConfiguration.getMendixObject().getId());
                        put("isActive", Boolean.TRUE);
                        put("loginType", loginType.toString());
                    }},
                    SPAttributeConsumingService.entityName,
                    SPAttributeConsumingService.MemberNames.SPAttributeConsumingService_SSOConfiguration.toString(),
                    SPAttributeConsumingService.MemberNames.isActive.toString(),
                    SPAttributeConsumingService.MemberNames.LoginType.toString()
            );
if(!spAttributeConsumingServices.isEmpty()) {
    IMendixObject obj = spAttributeConsumingServices.get(0);
    SPAttributeConsumingService spAttributeConsumingService = SPAttributeConsumingService.initialize(context, obj);
    index = spAttributeConsumingService.getindex();
}

        } catch (Exception e) {
            _logNode.error(e.getLocalizedMessage());
        }


        return  index;
    }
}
