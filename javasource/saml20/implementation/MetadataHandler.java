package saml20.implementation;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import org.opensaml.saml.common.SAMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import saml20.implementation.common.Constants;
import saml20.implementation.common.SAMLUtil;
import saml20.implementation.metadata.IdpMetadata;
import saml20.implementation.metadata.SPMetadataGenerator;
import saml20.implementation.security.CredentialRepository;
import saml20.proxies.SSOConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MetadataHandler extends SAMLHandler {
    private static final ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

    @Override
    public void handleRequest(SAMLRequestContext context) throws SAMLException {
        printTraceInfo(context);
        printMetadata(context);
    }

    private static void printMetadata(SAMLRequestContext context) throws SAMLException {
        SSOConfiguration ssoConfiguration = getSsoConfiguration(context);
        context.getResponse().setContentType("application/xml");
        try (OutputStream out = context.getResponse().getOutputStream();
             ByteArrayOutputStream stream = SPMetadataGenerator.generate(context.getIContext(),
                     context.getSpMetadata().getSpMetadataObject(),ssoConfiguration, CredentialRepository.getInstance())
        ) {
            out.write(stream.toByteArray());

        } catch (IOException e) {
            throw new SAMLException("Unable to write metadata back in the response", e);
        }
    }

    private static SSOConfiguration getSsoConfiguration(SAMLRequestContext context)  {
        String paths[] = context.getRequest().getResourcePath().split("/");
        if (paths.length >= 4) {
            return SAMLUtil.getSSOConfig(context.getIContext(),paths[3]);
        }
        return null;
    }
}
