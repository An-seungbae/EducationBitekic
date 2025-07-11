package oidc.implementation.security;

public class KeyStoreAlgInfo {
    private final String keyAlgorithm;
    private final int keySize;
    private final String signAlgorithm;
    private final boolean isEllipticCurveSignatureAlgorithm;
    private final String genParameterSpec;

    public KeyStoreAlgInfo(String keyAlgorithm, int keySize, String signAlgorithm, boolean isEllipticCurveSignatureAlgorithm, String genParameterSpec) {
        this.keyAlgorithm = keyAlgorithm;
        this.keySize = keySize;
        this.signAlgorithm = signAlgorithm;
        this.isEllipticCurveSignatureAlgorithm = isEllipticCurveSignatureAlgorithm;
        this.genParameterSpec = genParameterSpec;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public int getKeySize() {
        return keySize;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

    public boolean isEllipticCurveSignatureAlgorithm() {
        return isEllipticCurveSignatureAlgorithm;
    }

    public String getGenParameterSpec() {
        return genParameterSpec;
    }
}
