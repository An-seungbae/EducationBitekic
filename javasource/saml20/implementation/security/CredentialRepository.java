package saml20.implementation.security;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import saml20.implementation.common.Constants;
import saml20.implementation.metadata.IdpMetadata;
import saml20.implementation.metadata.IdpMetadata.Metadata;
import saml20.proxies.EncryptionKeyLength;
import saml20.proxies.EncryptionMethod;
import saml20.proxies.SPMetadata;
import saml20.proxies.SSOConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for managing credentials.
 */
public class CredentialRepository {
    private static final ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

    private final Map<Key, BasicX509Credential> credentials = new ConcurrentHashMap<Key, BasicX509Credential>();
    private static String jvmKeyStorePW = null;

    /**
     * The Trustore consists out of all Trusted Certificates from the JVM and the certificates included in the Model
     * During initialization all Certificates from the IdPs will be added to the Truststore as well.
     * <p>
     * The Idp publishes the TLS connection using on of the signing certificates
     */

    private KeyStore trustStoreSSL = null;
    /**
     * The KeyStore consists out of all Trusted Certificates from the JVM and the certificates included in the Model
     * During initialization the  private and PublicKey pair will added to the Truststore in order to perform
     * 'HolderOfKey' authentication
     */
    private KeyStore keyStoreSSL = null;

    private KeyStore keystoreIDPs;
    private boolean initialized = false;

    private static CredentialRepository _instance;

    public static CredentialRepository getInstance() throws SAMLException {
        if (_instance == null) {
            _instance = new CredentialRepository();
        }

        return _instance;
    }

    private CredentialRepository() {
    }

    public void updateConfiguration(IContext context, List<IMendixObject> ssoConfigurationList,IdpMetadata idpMetadata) throws SAMLException {
        Security.addProvider(new BouncyCastleProvider());
        this.keystoreIDPs = KeyStoreHelper.createKeyStore();
       for (IMendixObject ssoConfiguration : ssoConfigurationList) {
            SSOConfiguration ssoConfig = SSOConfiguration.initialize(context, ssoConfiguration);
            if(ssoConfig.getUseEncryption()){
                try {
                    saml20.proxies.KeyStore keyStore =  ssoConfig.getSSOConfiguration_KeyStore();
                    if(keyStore == null){
                        KeyStoreHelper.generateSelfKeyPair(ssoConfig,context);
                    }
                    KeyStoreHelper.updateCredential(ssoConfig,context);
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        // Reset the KeyStore and TrustStore because the  certificates potentially changed.
        this.keyStoreSSL = null;
        if (idpMetadata != null)
            setupTrustStore(idpMetadata);

        this.initialized = true;
    }

    /**
     * Load credentials from a keystore.
     * <p>
     * The first private key is loaded from the keystore.
     *
     * @param ssoConfig            Keystore and private key password.

     * @throws SAMLException
     */
    public BasicX509Credential getCredential(SSOConfiguration ssoConfig) throws SAMLException {
       /* if (!this.initialized)
            throw new SAMLException("The Credential Repository has not been initialized.");*/

        if (ssoConfig.getUseEncryption()) {
            Key key = null;
            try {
                key = new Key(ssoConfig.getSSOConfiguration_KeyStore().getPassword(), ssoConfig.getAlias());
            } catch (CoreException e) {
                return null;
            }
            BasicX509Credential credential = this.credentials.get(key);
            if (credential == null) {
                credential  =  KeyStoreHelper.updateCredential(ssoConfig, ssoConfig.getContext());
            }
            return credential;
        }

        return null;
    }

    /**
     * Prepares the TrustStore for the SSL connection. Loads the Platform KeyStore and extends it with all Certificates
     * from all IdPs
     *
     * @param idpMetadata
     * @throws SAMLException
     */
    private void setupTrustStore(IdpMetadata idpMetadata) throws SAMLException {
        try {

            this.trustStoreSSL = loadJVMKeyStore();

            for (Metadata metadata : idpMetadata.getAllMetaData()) {
                SecurityHelper.appendToIdPKeyStore(this.trustStoreSSL, metadata);
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new SAMLException("Unable to load the TrustStore ", e);
        }
    }

    public KeyStore getTrustStore() {
        return this.trustStoreSSL;
    }

    public KeyStore getKeystoreIDPs() {
        return this.keystoreIDPs;
    }

    /**
     * Sets up the SSL KeyStore by loading the initial Platform KeyStore and extending the KeyStore with all generated
     * SP Private/Public Key Pairs
     *
     * @return
     * @throws SAMLException
     */
    public KeyStore getSSLKeyStore() throws SAMLException {
        if (this.keyStoreSSL == null) {
            try {
                this.keyStoreSSL = loadJVMKeyStore();

                if (this.keystoreIDPs != null) {
                    Enumeration<String> aliasList = this.keystoreIDPs.aliases();
                    while (aliasList.hasMoreElements()) {
                        String alias = aliasList.nextElement();
                        this.keyStoreSSL.setEntry(alias, this.keystoreIDPs.getEntry(alias, new KeyStore.PasswordProtection(Constants.CERTIFICATE_PASSWORD.toCharArray())), new KeyStore.PasswordProtection(System.getProperty("javax.net.ssl.keyStorePassword").toCharArray()));
                    }
                }
            }catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableEntryException e) {
                throw new SAMLException("Unable to load the TrustStore ", e);
            }
        }

        return this.keyStoreSSL;
    }

    /**
     * Try and retrieve the initial KeyStore from the java.net.ssl KeyStore information. This is supposed to be the
     * Mendix Platform KeyStore
     *
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static KeyStore loadJVMKeyStore() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, IOException {
        KeyStore keyStore = KeyStore.getInstance("JKS");

        String keyStoreResource = System.getProperty("javax.net.ssl.keyStore");

        if (isValidFile(keyStoreResource)) {
            // applicable if certificates have been manually added to the server
            // (e.g. in the Mx modeler or in the cloud)
            File keyStoreFile = new File(keyStoreResource);

            if (jvmKeyStorePW == null) {
                jvmKeyStorePW = System.getProperty("javax.net.ssl.keyStorePassword");
            }

            try (FileInputStream keyStoreFileStream = new FileInputStream(keyStoreFile)) {
                keyStore.load(keyStoreFileStream, jvmKeyStorePW.toCharArray());
            }
        } else {
            // create a new store when no certificates have been manually added
            // to the server
            if (jvmKeyStorePW == null) {
                jvmKeyStorePW = saml20.proxies.constants.Constants.getKeystorePassword();
            }
            keyStore.load(null, jvmKeyStorePW.toCharArray());
        }
        return keyStore;
    }

    private static boolean isValidFile(String keyStoreResource){
        if(keyStoreResource == null || keyStoreResource.trim().isEmpty()){
            return false;
        }
        // Reject absolute paths
        Path inputPath = Paths.get(keyStoreResource);
        if (inputPath.isAbsolute()) {
            _logNode.info("Absolute paths are not allowed:"+keyStoreResource);
            return false;
        }

        // Normalize to remove ../ etc.
        Path normalized = inputPath.normalize();
        // Prevent path traversal (no ".." segments allowed)
        for (Path part : normalized) {
            if (part.toString().equals("..")) {
                _logNode.info("Path traversal attempt detected:"+keyStoreResource);
                return false;
            }
        }

        return true;
    }

    public void updateCredential(String password, String entityId, BasicX509Credential cred) throws SAMLException {
        Key key = new Key(password, entityId);
        this.credentials.put(key, cred);
        try {
            if(this.keystoreIDPs == null) {
                this.keystoreIDPs = KeyStoreHelper.createKeyStore();
            }
            this.keystoreIDPs.setKeyEntry(entityId, cred.getPrivateKey(), Constants.CERTIFICATE_PASSWORD.toCharArray(), new Certificate[]{cred.getEntityCertificate()});
        } catch (KeyStoreException e) {
            throw new SAMLException(e);
        }
    }

    private static class Key {
        private final String password;
        private final String entityId;

        public Key(String password, String entityId) {
            this.password = password;
            this.entityId = entityId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.password == null) ? 0 : this.password.hashCode());
            result = prime * result + ((this.entityId == null) ? 0 : this.entityId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (this.password == null) {
                if (other.password != null)
                    return false;
            } else if (!this.password.equals(other.password))
                return false;
            if (this.entityId == null) {
                if (other.entityId != null)
                    return false;
            } else if (!this.entityId.equals(other.entityId))
                return false;

            return true;
        }
    }

    public  String getJVMKeyStorePW(){
        return this.jvmKeyStorePW;
    }

}
