package oidc.implementation.security;

import com.nimbusds.jose.jwk.JWK;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JWKRepository {
    private final Map<String, JWK> jwks = new ConcurrentHashMap<>();
    private static JWKRepository _instance;

    private JWKRepository(){}

    public static JWKRepository getInstance(){
        if (_instance == null) {
            synchronized (JWKRepository.class){
                if (_instance == null) {
                    _instance = new JWKRepository();
                }
            }
        }
        return _instance;
    }

    public void addJWK(String alias,JWK jwk){
        this.jwks.put(alias,jwk);
    }

    public JWK getJWK(String alias){
       return this.jwks.get(alias);
    }
}
