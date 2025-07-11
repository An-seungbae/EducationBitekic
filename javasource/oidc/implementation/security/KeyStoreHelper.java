package oidc.implementation.security;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import communitycommons.StringUtils;
import oidc.implementation.common.Constants;
import oidc.proxies.ClientConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;


public class KeyStoreHelper {
    private final IContext context;
    private static final ILogNode _logNode = Core.getLogger(Constants.LOG_NODE);
    public KeyStoreHelper(IContext context){
        this.context =context;
    }
    public  JWK generateSelfKeyPair(ClientConfiguration ClientConfiguration) throws CoreException {
        _logNode.debug("generate new keypair");
        SecurityHelper securityHelper = new SecurityHelper();
        oidc.proxies.KeyStore KeyStore= ClientConfiguration.getClientConfiguration_KeyStore();
        if(KeyStore != null) {
            securityHelper.deleteExistingKeyStoreFile(KeyStore.getAlias());
            KeyStore.setInKeyStore_ClientConfiguration(ClientConfiguration);
            KeyStore.setClientConfiguration_KeyStore(null);
            Core.commit(context,KeyStore.getMendixObject());
            }

            String password = StringUtils.randomString(Constants.KEY_STORE_PASSWORD_SIZE);
            String alias = StringUtils.randomHash();
            IMendixObject keyStoreObj =   createKeyStoreEntity(ClientConfiguration,alias,password);
            KeyStore  ks = securityHelper.generateKeyStore(alias,password,ClientConfiguration.getJWT_Sign_Algorithm().name(),ClientConfiguration.getKeyPair_ExpirationDays());
            saveKeyStoreEntity(keyStoreObj);
        return loadJWK(ks,alias,password);
    }

    private  void saveKeyStoreEntity(IMendixObject   keyStoreObj) throws CoreException  {
        File file = new SecurityHelper().getFile(keyStoreObj.getValue(context,oidc.proxies.KeyStore.MemberNames.Alias.name()));
        try(FileInputStream fis = new FileInputStream(file)){
            Core.storeFileDocumentContent(context, keyStoreObj, fis);
        } catch (IOException e) {
            throw new CoreException(e);
        }
        Core.commit(context,keyStoreObj);
        _logNode.debug("saved Keystore object");
    }

    private  IMendixObject createKeyStoreEntity(ClientConfiguration ClientConfiguration, String alias, String password)  {
        oidc.proxies.KeyStore keyStore =new oidc.proxies.KeyStore(context);
        keyStore.setClientConfiguration_KeyStore(ClientConfiguration);
        keyStore.setAlias(alias);
        String encryptedPassword = encrypt(password);
        keyStore.setPassword(encryptedPassword);
        keyStore.setAlgorithm(ClientConfiguration.getJWT_Sign_Algorithm());
        return keyStore.getMendixObject();
    }

    private   String encrypt(String Plain){
        return Core.microflowCall("Encryption.Encrypt")
                .inTransaction(true)
                .withParam("Plain", Plain)
                .execute(context);
    }

    public   String decrypt(String encryption){
        return Core.microflowCall("Encryption.Decrypt")
                .inTransaction(true)
                .withParam("Encrypted", encryption)
                .execute(context);
    }

    public  JWK getJWK(ClientConfiguration ClientConfiguration) throws CoreException {
        JWK jwk = JWKRepository.getInstance().getJWK(ClientConfiguration.getAlias());
        if(jwk != null){
            return jwk;
        }
        _logNode.debug("Keys are not available in cache, update the cache of Keys");
        jwk = loadKeyStore(ClientConfiguration);
        if(jwk == null){
           // _logNode.error("Invalid JWK or JWK not found");
            throw new CoreException("Invalid JWK or JWK not found");
        }
        return jwk;
    }
    public  JWK getJWK(oidc.proxies.KeyStore keyStore) throws CoreException {
        String password = decrypt(keyStore.getPassword());
        String alg = keyStore.getAlgorithm().name();
        KeyStore ks = new SecurityHelper().getKeyStore(keyStore.getMendixObject(),password,alg, context);
        return loadJWK(ks, keyStore.getAlias(), password);
    }
    public  JWK loadKeyStore(ClientConfiguration ClientConfiguration) throws CoreException {
        oidc.proxies.KeyStore keyStore = ClientConfiguration.getClientConfiguration_KeyStore();
        String password = decrypt(keyStore.getPassword());
        String alg = ClientConfiguration.getJWT_Sign_Algorithm().name();
        KeyStore ks = new SecurityHelper().getKeyStore(keyStore.getMendixObject(),password,alg, context);
        return loadJWK(ks, keyStore.getAlias(), password);
    }

    private static JWK loadJWK(KeyStore ks,String alias, String password) throws CoreException {
        try{
            JWK  jwk= JWK.load(ks, alias, password.toCharArray());
            JWKRepository.getInstance().addJWK(alias,jwk);
            return jwk;
        }catch (KeyStoreException | JOSEException ex) {
            _logNode.error("Unable to load KeyStore:", ex);
            throw new CoreException("Unable to read Keystore file");
        }
    }
}
