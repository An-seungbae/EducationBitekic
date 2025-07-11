package oidc.implementation.security;

import com.auth0.jwt.algorithms.Algorithm;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import oidc.implementation.common.Constants;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
public class SecurityHelper {

    private static final String ALG_START_WITH_ES = "ES";
    private static final String ALG_START_WITH_PS = "PS";
    private static final String ALG_END_WITH_384 = "384";
    private static final String ALG_END_WITH_512 = "512";
    public static final String PROVIDER = "BC";
    public static final String JKS = "JKS";
    public static final String PKCS_12 = "PKCS12";
    private static final ILogNode _logNode = Core.getLogger(Constants.LOG_NODE);
    public SecurityHelper(){}


    public  KeyStore generateKeyStore(String alias,String password,String algo,long keyPairExpirationDays) throws CoreException {
        try{
            deleteExistingKeyStoreFile(alias);

            Security.addProvider(new BouncyCastleProvider());
            KeyStoreAlgInfo algInfo = getKeyStoreAlgInfo(algo);
            KeyPair keyPair = generateKeyPair(algInfo);

            X509Certificate certificate = generateCertificate(keyPair,algInfo,keyPairExpirationDays);
            return storeInKeystore(getFile(alias),password,alias,keyPair.getPrivate(),certificate);
        }catch (Exception ex){
            _logNode.error("Unable to generate credential:", ex);
            throw  new CoreException("Unable to generate credential");
        }


    }


    public  KeyStore getKeyStore(IMendixObject keyStoreObj,String password,String alg, IContext context) throws CoreException {
        try {
            KeyStore ks;
            File keystoreFile = deleteExistingKeyStoreFile(keyStoreObj.getValue(context,oidc.proxies.KeyStore.MemberNames.Alias.name()));
            try (InputStream inStr = Core.getFileDocumentContent(context, keyStoreObj);
                 FileOutputStream ous = new FileOutputStream(keystoreFile)) {
                int length = getKeyStoreAlgInfo(alg).getKeySize();
                ks = loadKeystore(inStr,password,length);
                IOUtils.copy(Core.getFileDocumentContent(context, keyStoreObj), ous);
                ous.flush();
            }
            return ks;
        }catch (Exception e){
             _logNode.error("Unable to read credential:", e);
             throw new CoreException("Unable to read credential");
        }

    }
    public  Algorithm getAlgorithm(oidc.proxies.ENU_SigningAlgorithm algorithm, JWK jwkObj) throws JOSEException, CoreException {
        RSAPublicKey pubKey = null;
        RSAPrivateKey privKey = null;
        ECPublicKey ecPublicKey = null;
        ECPrivateKey ecPrivateKey = null;

        if (jwkObj instanceof RSAKey) {
            pubKey = ((RSAKey) jwkObj).toRSAPublicKey();
            privKey = ((RSAKey) jwkObj).toRSAPrivateKey();
        }
        else if (jwkObj instanceof ECKey) {
            ecPublicKey = ((ECKey) jwkObj).toECPublicKey();
            ecPrivateKey = ((ECKey) jwkObj).toECPrivateKey();
        }
        return switch (algorithm) {
            case RS256, PS256 -> Algorithm.RSA256(pubKey, privKey);
            case RS384, PS384 -> Algorithm.RSA384(pubKey, privKey);
            case RS512, PS512 -> Algorithm.RSA512(pubKey, privKey);
            case ES256 -> Algorithm.ECDSA256(ecPublicKey, ecPrivateKey);
            case ES384 -> Algorithm.ECDSA384(ecPublicKey, ecPrivateKey);
            case ES512 -> Algorithm.ECDSA512(ecPublicKey, ecPrivateKey);
            default -> throw new CoreException("Unsupported algorithm: " + algorithm);
        };
    }
    public  File getFile(String alias){
        Path filePath = Paths.get(Core.getConfiguration().getTempPath().getAbsolutePath(),alias+"OIDCStore.keystore");
        return filePath.toFile();
    }

    private  KeyStore loadKeystore(InputStream input,String keystorePassword,int length) throws  KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keystore;
        input = new BufferedInputStream(input);
        input.mark(length * length);
        try {
            keystore = loadStore(input, keystorePassword, PKCS_12);
        } catch (IOException e) {
            input.reset();
            keystore = loadStore(input, keystorePassword, JKS);
        }
        return keystore;
    }

    private  KeyStore loadStore(InputStream input, String password, String type) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ks = KeyStore.getInstance(type);
        char[] jksPassword = password.toCharArray();
        ks.load(input, jksPassword);
        input.close();
        return ks;
    }


    private  KeyStoreAlgInfo getKeyStoreAlgInfo(String algo){
        String keyAlgorithm = Constants.RS_KEY_ALG;
        String  signAlgorithm= Constants.RS256_SIGN_ALGORITHM;
        int keySize = Constants.ALG_256_KEY_SIZE;
        String genParameterSpec="";
        boolean isECAlg = false,isPSAlg=false;
        if(algo.startsWith(ALG_START_WITH_ES)){
            keyAlgorithm = Constants.ES_KEY_ALG;
            signAlgorithm= Constants.ES256_SIGN_ALGORITHM;
            isECAlg = true;
            genParameterSpec = Constants.ES256_GEN_PARAMETER_SPEC;
        } else if (algo.startsWith(ALG_START_WITH_PS)){
            isPSAlg = true;
            signAlgorithm= Constants.PS256_SIGN_ALGORITHM;
        }
        if(algo.endsWith(ALG_END_WITH_384)){
            keySize = Constants.ALG_384_KEY_SIZE;
            signAlgorithm= Constants.RS384_SIGN_ALGORITHM;
            if(isECAlg){
                signAlgorithm= Constants.ES384_SIGN_ALGORITHM;
                genParameterSpec = Constants.ES384_GEN_PARAMETER_SPEC;
            } else if (isPSAlg) {
                signAlgorithm= Constants.PS384_SIGN_ALGORITHM;
            }

        } else if(algo.endsWith(ALG_END_WITH_512)){
            keySize = Constants.ALG_512_KEY_SIZE;
            signAlgorithm= Constants.RS512_SIGN_ALGORITHM;
            if(isECAlg){
                signAlgorithm= Constants.ES512_SIGN_ALGORITHM ;
                genParameterSpec = Constants.ES512_GEN_PARAMETER_SPEC ;
            } else if (isPSAlg) {
                signAlgorithm= Constants.PS512_SIGN_ALGORITHM;
            }
        }
        return new KeyStoreAlgInfo(keyAlgorithm,keySize,signAlgorithm,isECAlg,genParameterSpec);
    }


    private  KeyPair generateKeyPair(KeyStoreAlgInfo algoIfo) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(algoIfo.getKeyAlgorithm());

        if(algoIfo.isEllipticCurveSignatureAlgorithm()){
            //Generate Keypair for  ES256, ES384 or ES512 Alg
            keyGenerator.initialize(new ECGenParameterSpec(algoIfo.getGenParameterSpec()));
        }else{
            //Generate Keypair for RS512,RS256, RS384,PS256, PS384, or PS512 Alg
            keyGenerator.initialize(algoIfo.getKeySize());
        }
        _logNode.debug("new key pair generated ");
        return keyGenerator.generateKeyPair();

    }


    private  X509Certificate generateCertificate(KeyPair keyPair,KeyStoreAlgInfo algInfo,long keyPairExpirationDays) throws Exception {
        String cn = Core.getConfiguration().getApplicationRootUrl();
        X500Name issuer = new X500Name("CN="+cn+", O=Mendix-SP");
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Date startDate = new Date();
        Date endDate = new Date(System.currentTimeMillis() + (keyPairExpirationDays * 24 * 60 * 60 * 1000));

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serialNumber, startDate, endDate, issuer, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder(algInfo.getSignAlgorithm())
                .setProvider(PROVIDER)
                .build(keyPair.getPrivate());
        return new JcaX509CertificateConverter()
                .setProvider(PROVIDER)
                .getCertificate(certBuilder.build(signer));
    }

    private  KeyStore storeInKeystore(File keystorePath, String keystorePassword, String alias, PrivateKey privateKey, Certificate certificate) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(JKS);
        keyStore.load(null, keystorePassword.toCharArray()); // Create empty keystore
        keyStore.setKeyEntry(alias, privateKey, keystorePassword.toCharArray(), new Certificate[]{certificate});

        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, keystorePassword.toCharArray());
        }
        _logNode.debug("saved java Key Store");
        return keyStore;
    }


    public  File deleteExistingKeyStoreFile(String alias) {
        File keystoreFile = getFile(alias);
        if (keystoreFile.exists()) {
            if(!keystoreFile.delete()){
                _logNode.warn(String.format("Unable to delete the %s file:",keystoreFile.getName()));
            }
        }
        return keystoreFile;
    }
}
