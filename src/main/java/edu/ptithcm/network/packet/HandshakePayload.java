package edu.ptithcm.network.packet;

import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.security.Signable;

import javax.crypto.SecretKey;
import java.security.PrivateKey;


public class HandshakePayload implements Signable {
    private final String senderId;
    private final String encryptedSessionKey;
    private final long timestamp;
    private String signature;

    public HandshakePayload(String senderId, String encryptedSessionKey) {
        this.senderId = senderId;
        this.encryptedSessionKey = encryptedSessionKey;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public SecretKey decryptSessionKey(PrivateKey privateKey){
        String decryptedStr = CryptoUtils.decryptRSA(encryptedSessionKey, privateKey);
        return CryptoUtils.stringToSecretKey(decryptedStr);
    }

    @Override
    public String getSignableData() {
        return senderId + encryptedSessionKey + timestamp;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature;
    }
}
