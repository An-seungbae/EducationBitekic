package saml20.implementation;

import com.google.common.collect.ImmutableList;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import net.shibboleth.utilities.java.support.security.RandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.EncryptedIDUnmarshaller;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import saml20.implementation.common.Constants;
import saml20.implementation.metadata.IdpMetadata.Metadata;
import saml20.implementation.metadata.SPAttributeConsumingServicesUtils;
import saml20.proxies.EncryptionMethod;
import saml20.proxies.Enum_Attribute_Consuming_Login_Type;
import saml20.proxies.Enum_ProtocolBinding;
import saml20.proxies.SSOConfiguration;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Iterator;

public class OpenSAMLUtils {
    private static final ILogNode _logNode = Core.getLogger(Constants.LOGNODE);
    private static RandomIdentifierGenerationStrategy secureRandomIdGenerator;

    static {
        secureRandomIdGenerator = new RandomIdentifierGenerationStrategy();
    }

    public static <T> T buildSAMLObject(final Class<T> clazz) {
        T object = null;
        try {
            XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
            QName defaultElementName = (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
            object = (T) builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not create SAML object");
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Could not create SAML object");
        }

        return object;
    }

    public static String generateSecureRandomId() {
        return secureRandomIdGenerator.generateIdentifier();
    }

    public static NameID getNameIdFromEncryptedID(XMLObject value, Credential credential) {
        if(credential == null){
            return null;
        }
        KeyInfoCredentialResolver keyResolver = new StaticKeyInfoCredentialResolver(credential);

        ChainingEncryptedKeyResolver kekResolver = new ChainingEncryptedKeyResolver(
                ImmutableList.of(new InlineEncryptedKeyResolver(), new EncryptedElementTypeEncryptedKeyResolver(),
                        new SimpleRetrievalMethodEncryptedKeyResolver()));

        Decrypter decrypter = new Decrypter(null, keyResolver, kekResolver);

        NodeList elements = value.getDOM().getElementsByTagNameNS(SAMLConstants.SAML20_NS,
                EncryptedID.DEFAULT_ELEMENT_LOCAL_NAME);
        if (elements.getLength() > 0) {
            _logNode.debug("contains EncryptedID");

            Element element = (Element) elements.item(0);
            if (element != null) {
                // String responsexml = SerializeSupport.prettyPrintXML(element);
                // _logNode.debug("getNameIdFromEncryptedID: " + responsexml);
                EncryptedIDUnmarshaller m = new EncryptedIDUnmarshaller();
                EncryptedID encryptedID;
                try {
                    encryptedID = (EncryptedID) m.unmarshall(element);
                    return (NameID) decrypter.decrypt(encryptedID);

                } catch (UnmarshallingException e) {
                    _logNode.error("UnmarshallingException");
                    _logNode.error(e);
                } catch (DecryptionException e) {
                    _logNode.error("DecryptionException");
                    _logNode.error(e);
                }
            }
        }

        if (_logNode.isDebugEnabled())
            _logNode.debug("No EncryptedID");
        return null;
    }

    public static String getProtocolBinding(SAMLRequestContext context, Metadata metadata) {
        SSOConfiguration config = SSOConfiguration.initialize(context.getIContext(), metadata.getSsoConfiguration());
        if (config.getResponseProtocolBinding() == Enum_ProtocolBinding.ARTIFACT_BINDING)
            return SAMLConstants.SAML2_ARTIFACT_BINDING_URI;
        return SAMLConstants.SAML2_POST_BINDING_URI;
    }

    public static Integer getAttributeConsumingServiceIndex(SAMLRequestContext context, Metadata metadata,boolean isInSessionLogin) {
        SSOConfiguration config = SSOConfiguration.initialize(context.getIContext(), metadata.getSsoConfiguration());
       return SPAttributeConsumingServicesUtils.getAttributeConsumingServiceIndex(context.getIContext(),
               config,isInSessionLogin? Enum_Attribute_Consuming_Login_Type.InSession_Login :Enum_Attribute_Consuming_Login_Type.Initial_Login);
    }

    public static Document addSign(Document document, BasicX509Credential cert, EncryptionMethod encryptionMethod) {
        // Based on https://github.com/onelogin/java-saml/blob/master/core/src/main/java/com/onelogin/saml2/util/Util.java#L1406

        try {
            String signAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;//"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
            if (encryptionMethod.equals(EncryptionMethod.SHA1withRSA)) {
                signAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
            }

            String digestAlgorithm = SignatureConstants.ALGO_ID_DIGEST_SHA256;//http://www.w3.org/2001/04/xmlenc#sha256";
            String c14nMethod = SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;//"http://www.w3.org/2001/10/xml-exc-c14n#";
            String transformSignature = SignatureConstants.TRANSFORM_ENVELOPED_SIGNATURE;//"http://www.w3.org/2000/09/xmldsig#enveloped-signature";


            document.normalizeDocument();

            //Fixed Digest value of signature
            String xmlStr =  SerializeSupport.nodeToString(document.getDocumentElement());
            document = convertStringToDocument(xmlStr);

            // Signature object
            XMLSignature sig = new XMLSignature(document, null, signAlgorithm, c14nMethod);

            // Including the signature into the document before sign, because
            // this is an envelop signature
            Element root = document.getDocumentElement();
            document.setXmlStandalone(false);

            // If Issuer, locate Signature after Issuer, Otherwise as first child.
            NodeList issuerNodes = query(document, "//saml:Issuer", null);
            Element elemToSign = null;
            if (issuerNodes.getLength() > 0) {
                Node issuer =  issuerNodes.item(0);
                root.insertBefore(sig.getElement(), issuer.getNextSibling());
                elemToSign = (Element) issuer.getParentNode();
            } else {
                NodeList entitiesDescriptorNodes = query(document, "//md:EntitiesDescriptor", null);
                if (entitiesDescriptorNodes.getLength() > 0) {
                    elemToSign = (Element)entitiesDescriptorNodes.item(0);
                } else {
                    NodeList entityDescriptorNodes = query(document, "//md:EntityDescriptor", null);
                    if (entityDescriptorNodes.getLength() > 0) {
                        elemToSign = (Element)entityDescriptorNodes.item(0);
                    } else {
                        elemToSign = root;
                    }
                }
                root.insertBefore(sig.getElement(), elemToSign.getFirstChild());
            }

            String id = elemToSign.getAttribute("ID");
            String reference = id;
            if (!id.isEmpty()) {
                elemToSign.setIdAttributeNS(null, "ID", true);
                reference = "#" + id;
            }

            // Create the transform for the document
            Transforms transforms = new Transforms(document);
            transforms.addTransform(transformSignature);
            transforms.addTransform(c14nMethod);
            sig.addDocument(reference, transforms, digestAlgorithm);

            // Add the certification info
            sig.addKeyInfo(cert.getEntityCertificate());

            // Sign the document
            sig.sign(CredentialSupport.extractSigningKey(cert));

            // Replacing the carriage return ("\r\n") with "" in the "ds:SignatureValue" node
            Node signatureValueNode = sig.getElement().getElementsByTagName("ds:SignatureValue").item(0);
            if(signatureValueNode != null && signatureValueNode.getTextContent().contains("\r\n"))
            {
                signatureValueNode.setTextContent(signatureValueNode.getTextContent().replaceAll("\r\n",""));
            }

            Node certificateValueNode = sig.getElement().getElementsByTagName("ds:X509Certificate").item(0);
            if(certificateValueNode != null && certificateValueNode.getTextContent().contains("\r\n")) {
                certificateValueNode.setTextContent(Base64.getEncoder().encodeToString(cert.getEntityCertificate().getEncoded()));

            }
        } catch (XMLSecurityException|XPathExpressionException | ParserConfigurationException | CertificateEncodingException e) {
                        _logNode.error("Unable add signature to sp-metadata", e);
                    } catch (IOException | SAXException e) {
            _logNode.error("Unable add signature to sp-metadata",e);
        }


        return document;
    }

    public static NodeList query(Document dom, String query, Node context) throws XPathExpressionException {
        NodeList nodeList;
        XPath xpath = getXPathFactory().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {

            @Override
            public String getNamespaceURI(String prefix) {
                String result = null;
                if (prefix.equals("samlp") || prefix.equals("samlp2")) {
                    result =  "urn:oasis:names:tc:SAML:2.0:protocol";
                } else if (prefix.equals("saml") || prefix.equals("saml2")) {
                    result = "urn:oasis:names:tc:SAML:2.0:assertion";
                } else if (prefix.equals("ds")) {
                    result = "http://www.w3.org/2000/09/xmldsig#";
                } else if (prefix.equals("xenc")) {
                    result = "http://www.w3.org/2001/04/xmlenc#";
                } else if (prefix.equals("md")) {
                    result = "urn:oasis:names:tc:SAML:2.0:metadata";
                }
                return result;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @SuppressWarnings("rawtypes")
            @Override
            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        });

        if (context == null) {
            nodeList = (NodeList) xpath.evaluate(query, dom, XPathConstants.NODESET);
        } else {
            nodeList = (NodeList) xpath.evaluate(query, context, XPathConstants.NODESET);
        }
        return nodeList;
    }

    private static XPathFactory getXPathFactory() {
        try {
            /*
             * Since different environments may return a different XPathFactoryImpl, we should try to initialize the factory
             * using specific implementation that way the XML is parsed in an expected way.
             *
             * We should use the standard XPathFactoryImpl that comes standard with Java.
             *
             * NOTE: We could implement a check to see if the "javax.xml.xpath.XPathFactory" System property exists and is set
             *       to a value, if people have issues with using the specified implementor. This would allow users to always
             *       override the implementation if they so need to.
             */
            return XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl", ClassLoader.getSystemClassLoader());
        } catch (XPathFactoryConfigurationException e) {
            // LOGGER.debug("Error generating XPathFactory instance: " + e.getMessage(), e);
        }

        /*
         * If the expected XPathFactory did not exist, we fallback to loading the one defined as the default.
         *
         * If this is still throwing an error, the developer can set the "javax.xml.xpath.XPathFactory" system property
         * to specify the default XPathFactoryImpl implementation to use. For example:
         *
         * -Djavax.xml.xpath.XPathFactory:http://java.sun.com/jaxp/xpath/dom=net.sf.saxon.xpath.XPathFactoryImpl
         * -Djavax.xml.xpath.XPathFactory:http://java.sun.com/jaxp/xpath/dom=com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl
         *
         */
        return XPathFactory.newInstance();
    }

    public static Document convertStringToDocument(String xmlStr) throws ParserConfigurationException, IOException, SAXException {
               DocumentBuilderFactory factory = getDocumentBuilderFactory();
                DocumentBuilder builder =factory.newDocumentBuilder();
               return builder.parse(new ByteArrayInputStream(xmlStr.getBytes("UTF-8"))); //remove the parameter UTF-8 if you don't want to specify the Encoding type.
            }
    public static TransformerFactory getTransformerFactory(){
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        return transformerFactory;
    }
    public static DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory;
    }
}
