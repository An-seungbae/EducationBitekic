package saml20.implementation.binding;

import java.security.SecureRandom;
import java.util.Base64;
import org.apache.velocity.VelocityContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;

public class MxSAMLHTTPPostEncoder extends HTTPPostEncoder {
	protected void populateVelocityContext(final VelocityContext velocityContext,
			final MessageContext<SAMLObject> messageContext, final String endpointURL) throws MessageEncodingException {
		super.populateVelocityContext(velocityContext, messageContext, endpointURL);
		String nonce = generateRandomNonce();
		velocityContext.put("nonce", nonce);
	}

	private String generateRandomNonce() {
		SecureRandom secureRandom = new SecureRandom();
		byte[] nonceBytes = new byte[16];
		secureRandom.nextBytes(nonceBytes);
		return Base64.getEncoder().encodeToString(nonceBytes);
	}
}