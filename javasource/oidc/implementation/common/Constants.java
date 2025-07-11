package oidc.implementation.common;

public interface Constants {

     String DISCOVERY_ATTRIBUTE = "idp";
    String SSO_PATH = "oauth/v2/login";
     String SSO_CONTINUATION_PARAMETER = "cont";
     String ATTRIBUTE_IDPLIST = "idpList";
    String ATTRIBUTE_CONT = "cont";

    String LOG_NODE = "OIDC";

    String RS_KEY_ALG = "RSA";
    String ES_KEY_ALG = "EC";
    int ALG_256_KEY_SIZE = 2048;
    int ALG_384_KEY_SIZE = 3072;
    int ALG_512_KEY_SIZE = 4096;

    int KEY_STORE_PASSWORD_SIZE = 16;
    String RS256_SIGN_ALGORITHM = "SHA256withRSA";
    String RS384_SIGN_ALGORITHM = "SHA384withRSA";
    String RS512_SIGN_ALGORITHM = "SHA512withRSA";
    String PS256_SIGN_ALGORITHM = "SHA256withRSAandMGF1";
    String PS384_SIGN_ALGORITHM = "SHA384withRSAandMGF1";
    String PS512_SIGN_ALGORITHM = "SHA512withRSAandMGF1";
    String ES256_SIGN_ALGORITHM = "SHA256withECDSA";
    String ES256_GEN_PARAMETER_SPEC = "secp256r1";
    String ES384_SIGN_ALGORITHM = "SHA384withECDSA";
    String ES384_GEN_PARAMETER_SPEC = "secp384r1";
    String ES512_SIGN_ALGORITHM = "SHA512withECDSA";
    String ES512_GEN_PARAMETER_SPEC = "secp521r1";
}