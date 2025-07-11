package saml20.implementation.security;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixIdentifier;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xml.security.algorithms.JCEMapper;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.security.x509.BasicX509Credential;
import saml20.implementation.common.Constants;
import saml20.implementation.metadata.IdpMetadata.Metadata;
import saml20.proxies.EncryptionKeyLength;
import saml20.proxies.EncryptionMethod;
import saml20.proxies.SPMetadata;
import system.proxies.FileDocument;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

/**
 * Some utility methods for building and loading certificates and the KeyStore
 */
public class SecurityHelper {

    private static ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

    /**
     * Randomly generates a Java JCE KeyPair object from the specified XML Encryption algorithm URI.
     *
     * @param algoURI   The XML Encryption algorithm URI
     * @param keyLength the length of key to generate
     * @return a randomly-generated KeyPair
     * @throws NoSuchProviderException  provider not found
     * @throws NoSuchAlgorithmException algorithm not found
     */
    public static KeyPair generateKeyPairFromURI(String algoURI, int keyLength) throws NoSuchAlgorithmException, NoSuchProviderException {
        String jceAlgorithmName = JCEMapper.getJCEKeyAlgorithmFromURI(algoURI);
        return generateKeyPair(jceAlgorithmName, keyLength);
    }

    /**
     * Generate a random asymmetric key pair.
     *
     * @param algo      key algorithm
     * @param keyLength key length
     * @return randomly generated key
     * @throws NoSuchAlgorithmException algorithm not found
     */
    private static KeyPair generateKeyPair(String algo, int keyLength) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(algo);
        keyGenerator.initialize(keyLength);
        return keyGenerator.generateKeyPair();
    }

    public static X509Certificate generateCertificate(EncryptionMethod encrMethod, KeyPair keyPair, String entityId) throws Exception {
        X500Name issuer = new X500Name("cn=" + entityId + ", ou=Mendix-SP");
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L); // Change the date to yesterday
        // to prevent any accidental
        // timezone issues
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 365L * 10L);
        X500Name subject = new X500Name("cn=" + entityId + ", ou=Mendix-SP");

        @SuppressWarnings("resource")
        SubjectPublicKeyInfo publicKeyInfo;
        try (ByteArrayInputStream bIn = new ByteArrayInputStream(keyPair.getPublic().getEncoded());
             ASN1InputStream sequence = new ASN1InputStream(bIn)) {
            publicKeyInfo = new SubjectPublicKeyInfo((ASN1Sequence) sequence.readObject());
        }

        X509v3CertificateBuilder gen = new X509v3CertificateBuilder(issuer, serialNumber, notBefore, notAfter, subject, publicKeyInfo);

        gen.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));
        gen.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.getPublic()));

        ContentSigner sigGen = new JcaContentSignerBuilder((encrMethod != null ? encrMethod.toString() : EncryptionMethod.SHA1withRSA.toString()))
                .setProvider("BC").build(keyPair.getPrivate());
        X509CertificateHolder certificateHolder = gen.build(sigGen);

        X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);
        return x509Certificate;
    }


    private static KeyStore loadStore(InputStream input, String password, String type) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ks = KeyStore.getInstance(type);
        char[] jksPassword = password.toCharArray();
        ks.load(input, jksPassword);
        input.close();
        return ks;
    }


    public static KeyStore appendToIdPKeyStore(KeyStore ks, Metadata idpMetadata) throws SAMLException {
        try {
            if (ks == null) {
                ks = KeyStore.getInstance("JKS");
                ks.load(null, null);
            }

            String conflictingCertificates = addAllToKeyStore(ks, idpMetadata.getCertificates());
            conflictingCertificates += addAllToKeyStore(ks, idpMetadata.getSigningCertificates());

            if (!"".equals(conflictingCertificates))
                throw new SAMLException("Unable to load the IdP Keystore, the following certificates are conflicting for Idp: " + idpMetadata.getEntityID() + " - " + conflictingCertificates);
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            throw new SAMLException(e);
        }

        return ks;
    }

    /**
     * Returns string of certificates that were conflicting
     */
    private static String addAllToKeyStore(KeyStore ks, Collection<X509Certificate> certificates)
            throws CertificateEncodingException, KeyStoreException {
        String conflictingCertificates = "";
        for (X509Certificate cert : certificates) {
            // thumbprint, see https://stackoverflow.com/questions/1270703/how-to-retrieve-compute-an-x509-certificates-thumbprint-in-java
            String alias = DigestUtils.sha256Hex(cert.getEncoded()) + "|" + cert.getVersion() + "|" + cert.getSerialNumber();
            String friendlyName = cert.getSubjectDN().getName() + "::" + cert.getIssuerDN().getName();

            if (ks.containsAlias(alias)) {
                if (!ks.getCertificate(alias).equals(cert)) {
                    conflictingCertificates += friendlyName + "|";
                } else {
                    _logNode.warn("The following certificate is being used twice: " + friendlyName);
                }
            }
            ks.setCertificateEntry(alias, cert);
        }
        return conflictingCertificates;
    }
}
