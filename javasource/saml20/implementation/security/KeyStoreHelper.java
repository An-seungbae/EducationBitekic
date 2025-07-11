package saml20.implementation.security;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.security.x509.BasicX509Credential;
import saml20.implementation.common.Constants;
import saml20.implementation.common.SAMLUtil;
import saml20.proxies.EncryptionKeyLength;
import saml20.proxies.SPMetadata;
import saml20.proxies.SSOConfiguration;
import system.proxies.FileDocument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class KeyStoreHelper {
    private static final ILogNode _logNode = Core.getLogger(Constants.LOGNODE);

    public static KeyStore getKeystore(InputStream input, EncryptionKeyLength encryptionKeyLength, String password) throws  KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keystore;
        input = new BufferedInputStream(input);

        int length = (encryptionKeyLength == EncryptionKeyLength._2048bit_Encryption ? 2048 : 1024);
        input.mark(length * length);

        if (password == null) {
            password = Constants.CERTIFICATE_PASSWORD;
        }
        try {
            keystore = loadStore(input, password, "PKCS12");
        } catch (IOException e) {
            _logNode.debug("Keystore is not of type 'PCKS12' Trying type 'JKS'. (" + e.getMessage() + ")");
            input.reset();
            keystore = loadStore(input, password, "JKS");
        }

        return keystore;
    }

    public static  String encrypt(String Plain,IContext context){
        return Core.microflowCall("Encryption.Encrypt")
                .inTransaction(true)
                .withParam("Plain", Plain)
                .execute(context);
    }

    public static  String decrypt(String encryption,IContext context){
        return Core.microflowCall("Encryption.Decrypt")
                .inTransaction(true)
                .withParam("Encrypted", encryption)
                .execute(context);
    }
    private static KeyStore loadStore(InputStream input, String password, String type) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ks = KeyStore.getInstance(type);
        char[] jksPassword = password.toCharArray();
        ks.load(input, jksPassword);
        input.close();
        return ks;
    }

    public static void generateSelfKeyPair(SSOConfiguration aSSOConfiguration, IContext context) {
        try {
            IMendixObject keyStoreObj = aSSOConfiguration.getSSOConfiguration_KeyStore() != null ? aSSOConfiguration.getSSOConfiguration_KeyStore().getMendixObject() : null;
            int length = (aSSOConfiguration.getEncryptionKeyLength().equals(EncryptionKeyLength._2048bit_Encryption) ? 2048 : 1024);
            String alias = aSSOConfiguration.getAlias();

            deleteExistingKeyStoreFile();

            KeyPair kp = SecurityHelper.generateKeyPairFromURI("http://www.w3.org/2001/04/xmlenc#rsa-1_5", length);
            KeyStore ks = createKeyStore();
            IMendixObject spMetadataConfiguration = SAMLUtil.getMetadataConfig(context);
            String entityId = spMetadataConfiguration.getValue(context, SPMetadata.MemberNames.EntityID.toString());
            Security.addProvider(new BouncyCastleProvider());
            X509Certificate cert = SecurityHelper.generateCertificate(aSSOConfiguration.getEncryptionMethod(), kp,
                    entityId);

            BasicX509Credential cred = new BasicX509Credential(cert, kp.getPrivate());

            // (JPU) added March 18 2015 to fix bug, certificate not updated in sp_metadata.xml when changing
            // certificate encryption methods.
            CredentialRepository.getInstance().updateCredential(Constants.CERTIFICATE_PASSWORD, alias, cred);

            ks.setKeyEntry(entityId, cred.getPrivateKey(), Constants.CERTIFICATE_PASSWORD.toCharArray(), new Certificate[]{cert});

            saveKeystore(ks);

            saveKeyStoreEntity(aSSOConfiguration, context, keyStoreObj, alias, entityId);
        } catch (Exception e) {
            _logNode.error("Unable to generate KeyStore", e);
            throw new RuntimeException("Unable to generate KeyStore", e);
        }
    }

    private static void saveKeyStoreEntity(SSOConfiguration aSSOConfiguration, IContext context, IMendixObject keyStoreObj, String alias, String entityId) throws SAMLException {

        if (keyStoreObj == null) {
            keyStoreObj = Core.instantiate(context, saml20.proxies.KeyStore.entityName);
            keyStoreObj.setValue(context, FileDocument.MemberNames.Name.toString(), String.format("%SData.jks", alias));
            keyStoreObj.setValue(context, saml20.proxies.KeyStore.MemberNames.SSOConfiguration_KeyStore.toString(), aSSOConfiguration.getMendixObject().getId());
            String encryptedPassword = encrypt(Constants.CERTIFICATE_PASSWORD, context);
            keyStoreObj.setValue(context, saml20.proxies.KeyStore.MemberNames.Password.toString(), encryptedPassword);
        }
        keyStoreObj.setValue(context, saml20.proxies.KeyStore.MemberNames.LastChangedOn.toString(), new Date());
        keyStoreObj.setValue(context, saml20.proxies.KeyStore.MemberNames.Alias.toString(), entityId);
        try(FileInputStream fis = new FileInputStream(Constants.CERTIFICATE_LOCATION)){
            Core.storeFileDocumentContent(context, keyStoreObj, fis);
        } catch (IOException e) {
            _logNode.error("Unable to save KeyStore :"+ e.getMessage());
            throw new SAMLException(e);
        }


    }

    private static void saveKeystore(KeyStore ks) throws  SAMLException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ks.store(bos, Constants.CERTIFICATE_PASSWORD.toCharArray());
            bos.writeTo(Files.newOutputStream(Paths.get(Constants.CERTIFICATE_LOCATION)));
            bos.flush();
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            _logNode.error("Unable to save KeyStore : " + e.getMessage());
            throw new SAMLException(e);
        }
    }

    private static void deleteExistingKeyStoreFile() {
        File keystoreFile = new File(Constants.CERTIFICATE_LOCATION);
        if (keystoreFile.exists()) {
            boolean isDelete =  keystoreFile.delete();
            if(!isDelete){
                _logNode.warn("Unable to delete the key store file");
            }
        }
    }

    public static KeyStore createKeyStore() {
        try {
            KeyStore   ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            return ks;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            _logNode.error("Unable to create KeyStore", e);
            throw new RuntimeException(e);
        }


    }

    public static BasicX509Credential updateCredential(SSOConfiguration aSSOConfiguration,IContext context){
        try {
            IMendixObject keyStoreObj = aSSOConfiguration.getSSOConfiguration_KeyStore().getMendixObject();
            KeyStore ks;
            File keystoreFile = new File(Constants.CERTIFICATE_LOCATION);
            if (keystoreFile.exists()) {
                boolean isDelete =  keystoreFile.delete();
                if(!isDelete){
                    _logNode.warn("Unable to delete the key store file");
                }
            }

            String password = keyStoreObj.getValue(context, saml20.proxies.KeyStore.MemberNames.Password.toString());
            if (password != null) {
                password = KeyStoreHelper.decrypt(password, context);
            } else {
                password = Constants.CERTIFICATE_PASSWORD;
            }

            try (InputStream inStr = Core.getFileDocumentContent(context, keyStoreObj);
                 FileOutputStream ous = new FileOutputStream(keystoreFile)) {

                ks = getKeystore(inStr, aSSOConfiguration.getEncryptionKeyLength(), password);
                IOUtils.copy(Core.getFileDocumentContent(context, keyStoreObj), ous);
                ous.flush();
            }

            String alias = (String) keyStoreObj.getValue(context, saml20.proxies.KeyStore.MemberNames.Alias.toString());

            // (JPU) added April 16 2015 to fix bug, certificate not updated in sp_metadata.xml when
            // uploading own key store file + setting credential values to include key store private and
            // public key.
            X509Certificate ksCert = (X509Certificate) ks.getCertificate(alias);

            // FIXME: should we use the keystore adapter here?
            //  KeyStoreX509CredentialAdapter cred = new KeyStoreX509CredentialAdapter(ks, alias, Constants.CERTIFICATE_PASSWORD.toCharArray());
            if (ksCert == null) {
                throw new SAMLException("Unable to load the certificate from the key store. If you have just added your own key store make sure the alias is equal to the entity ID of the SP (currently: " + alias + ") and add your key store again.");
            } else {
                BasicX509Credential cred = new BasicX509Credential(ksCert);
                try {
                    cred.setPrivateKey((PrivateKey) ks.getKey(alias, password.toCharArray()));
                } catch (Exception e) {
                    throw new SAMLException("Unable to load the private key from the key store. If you have just added your own key store make sure the key store password is equal to the password configured in the model and add your key store again.");
                }
                cred.setEntityCertificate(ksCert);
//               cred.setPublicKey(ksCert.getPublicKey()); // FIXME: this is not allowed on an X509 credential
                CredentialRepository.getInstance().updateCredential(Constants.CERTIFICATE_PASSWORD, aSSOConfiguration.getAlias(), cred);
                return cred;
            }

        }catch (Exception e){
            _logNode.error("Unable to generate credential", e);
        }
        return null;
    }
}
