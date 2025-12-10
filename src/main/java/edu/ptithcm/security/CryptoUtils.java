package edu.ptithcm.security;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtils {

    // --- PHẦN 1: HASHING (Băm dữ liệu) ---
    // Dùng để băm mật khẩu hoặc tạo checksum file
    public static String hashSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- PHẦN 2: AES (Mã hóa đối xứng - Dùng để mã hóa nội dung chat) ---

    // 2.1 Tạo khóa AES (Trả về đối tượng SecretKey)
    public static SecretKey generateAESKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 2.2 Mã hóa AES (Nhận SecretKey trực tiếp)
    public static String encryptAES(String plainText, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] byteCipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(byteCipherText);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 2.3 Giải mã AES (Nhận SecretKey trực tiếp)
    public static String decryptAES(String encryptedText, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] bytePlainText = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(bytePlainText, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 2.4 Convert SecretKey -> String (Để gửi qua mạng)
    public static String secretKeyToString(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    // 2.5 Convert String -> SecretKey (Để nhận từ mạng về dùng)
    public static SecretKey stringToSecretKey(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    // --- PHẦN 3: RSA (Mã hóa bất đối xứng - Dùng cho Chữ ký số & Trao đổi khóa AES) ---

    // Tạo cặp khóa RSA (Public & Private)
    public static KeyPair generateRSAKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048); // Độ dài khóa an toàn
            return kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Convert Public Key sang String (để gửi cho bạn chat)
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    // Convert String sang Public Key (để đọc key của bạn chat)
    public static PublicKey stringToPublicKey(String keyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Convert Private Key sang String (để lưu vào file nếu cần)
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    // Convert String sang Private Key
    public static PrivateKey stringToPrivateKey(String keyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encryptRSA(String plainText, PublicKey publicKey){
        try{
            byte[] textBytes = plainText.getBytes(StandardCharsets.UTF_8);
            Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = encryptCipher.doFinal(textBytes);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptRSA(String encryptedText, PrivateKey privateKey){
        try{
            Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = decryptCipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    // --- PHẦN 4: CHỮ KÝ SỐ (Digital Signature) ---

    // Ký dữ liệu (Dùng Private Key của mình để ký)
    public static String sign(String plainText, PrivateKey privateKey) {
        try {
            Signature privateSignature = Signature.getInstance("SHA256withRSA");
            privateSignature.initSign(privateKey);
            privateSignature.update(plainText.getBytes("UTF-8"));
            byte[] signature = privateSignature.sign();
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Xác thực chữ ký (Dùng Public Key của người gửi để kiểm tra)
    public static boolean verify(String plainText, String signature, PublicKey publicKey) {
        try {
            Signature publicSignature = Signature.getInstance("SHA256withRSA");
            publicSignature.initVerify(publicKey);
            publicSignature.update(plainText.getBytes("UTF-8"));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return publicSignature.verify(signatureBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void main(){
        String data = "Hello, this is plain text";

        var keypair = CryptoUtils.generateRSAKeyPair();
        var pubKey = keypair.getPublic();
        var privKey = keypair.getPrivate();

        IO.println(decryptRSA(encryptRSA(data, pubKey), privKey));

        String signature = CryptoUtils.sign(data, keypair.getPrivate());
        boolean verify_sig = CryptoUtils.verify(data, signature, keypair.getPublic());

        IO.println(data);
        IO.println(signature);
        IO.println(verify_sig);
        IO.println(CryptoUtils.privateKeyToString(keypair.getPrivate()));
        IO.println(CryptoUtils.publicKeyToString(keypair.getPublic()));
        IO.println(CryptoUtils.hashSHA256(CryptoUtils.publicKeyToString(keypair.getPublic())));
    }
}