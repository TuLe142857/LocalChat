package edu.ptithcm.security;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface Signable {

    String getSignableData();
    String getSignature();
    void setSignature(String signature);

    // --- DEFAULT METHODS (Logic chung) ---
    default void sign(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private Key cannot be null");
        }
        String data = getSignableData();
        String sig = CryptoUtils.sign(data, privateKey);
        setSignature(sig);
    }

    default boolean verify(PublicKey publicKey) {
        String sig = getSignature();
        if (publicKey == null || sig == null || sig.isBlank()) {
            return false;
        }
        String data = getSignableData();
        return CryptoUtils.verify(data, sig, publicKey);
    }
}