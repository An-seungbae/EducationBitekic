package saml20.implementation;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import org.opensaml.saml.common.SAMLException;
import saml20.implementation.common.Constants;
import saml20.implementation.common.HTTPUtils;
import saml20.implementation.common.SAMLUtil;
import saml20.implementation.metadata.SPMetadataGenerator;
import saml20.implementation.security.CredentialRepository;
import saml20.proxies.SSOConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SubmitLoginFormJSHandler extends SAMLHandler {
    private static final ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

    @Override
    public void handleRequest(SAMLRequestContext context) throws SAMLException {
     try{
         HTTPUtils.SubmitLoginForm(context.getResponse());
     } catch (IOException e) {
         _logNode.error("Unable to render Javascript", e);
     }
    }




}
