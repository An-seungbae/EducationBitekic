package oidc.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.nimbusds.jose.jwk.JWK;
import oidc.implementation.common.Constants;
import oidc.implementation.common.OIDCUtils;
import oidc.implementation.security.KeyStoreHelper;
import oidc.proxies.ClientConfiguration;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class MxOIDCHandler {

    private static MxOIDCHandler _instance;

    private final ReentrantLock lock = new ReentrantLock();

    private long cacheUpdatedTime;
    private static final ILogNode _logNode = Core.getLogger(Constants.LOG_NODE);

    public static MxOIDCHandler getInstance(){
        if (_instance == null) {
            synchronized (MxOIDCHandler.class) {
                if (_instance == null) {
                    _instance = new MxOIDCHandler();
                }
            }
        }
        return _instance;
    }

    public  JWK getJWK(ClientConfiguration ClientConfiguration, IContext context) throws CoreException {
        reloadConfiguration(context);
        KeyStoreHelper keyStoreHelper =new KeyStoreHelper(context);
        JWK jwk = keyStoreHelper.getJWK(ClientConfiguration);
        if(jwk.getExpirationTime().before(new Date())){
            _logNode.debug("credentials expired");
            return generateSelfKeyPair(keyStoreHelper,ClientConfiguration);
        }
        return jwk;
    }

    public void reloadConfiguration(IContext context) throws CoreException{
        long cacheTime = OIDCUtils.getCacheUpdatedTime(context);
        if(cacheUpdatedTime < cacheTime){
            _logNode.debug("reloadConfiguration");
            KeyStoreHelper keyStoreHelper = new KeyStoreHelper(context);
            List<IMendixObject> ssoConfigurationList =  OIDCUtils.getPrivateKeySSOConfig(context);
            for(IMendixObject ssoConfiguration :ssoConfigurationList){
                ClientConfiguration clientConfiguration = ClientConfiguration.initialize(context,ssoConfiguration);
                keyStoreHelper.loadKeyStore(clientConfiguration);
            }
            cacheUpdatedTime = cacheTime;
        }
    }

    private JWK generateSelfKeyPair(KeyStoreHelper keyStoreHelper,ClientConfiguration ClientConfiguration) throws CoreException{
        lock.lock();
        try {
            _logNode.debug("locked Keypair generation");
            JWK jwk = keyStoreHelper.getJWK(ClientConfiguration);
            if (jwk.getExpirationTime().before(new Date())) {
                return keyStoreHelper.generateSelfKeyPair(ClientConfiguration);
            }
            return jwk;
        }finally {
            lock.unlock();
            _logNode.debug("unlocked Keypair generation");
        }
    }
}
