package edu.ptithcm.model;

import edu.ptithcm.security.CredentialManager;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.util.JsonUtils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * this class is immutable.<br>
 * id = HashSHA256(publickey).
 */
public class Credential {
    private final String id;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public void setName(String name) {
        this.name = name;
    }

    private String name;

    public Credential(KeyPair keyPair, String name){
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        this.name = name;
        this.id = CryptoUtils.hashSHA256(CryptoUtils.publicKeyToString(this.publicKey));
    }

    public Credential(PublicKey publicKey, PrivateKey privateKey, String name){
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.name = name;
        this.id = CryptoUtils.hashSHA256(CryptoUtils.publicKeyToString(this.publicKey));
    }

    public String getId() {
        return id;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getName() {
        return name;
    }

    static void main(){
        Credential credential = new Credential(CryptoUtils.generateRSAKeyPair(), "tú nè");
        String json = JsonUtils.toJson(credential);
        Credential credential1 = JsonUtils.fromJson(json, Credential.class);
        IO.println(json);
        IO.println(JsonUtils.toJson(credential1));

        String plain = "hello";
        String cipher = CryptoUtils.encryptRSA(plain, credential.getPublicKey());
        String decrypt = CryptoUtils.decryptRSA(cipher, credential1.getPrivateKey());
        IO.println(plain);
        IO.println(cipher);
        IO.println(decrypt);
        CredentialManager.writeCredentialToFile(credential);
        Credential credential2 = CredentialManager.readStoredCredential();
        IO.println(CryptoUtils.decryptRSA(cipher, credential2.privateKey));
    }
}
