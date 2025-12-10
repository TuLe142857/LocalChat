package edu.ptithcm.security;

import edu.ptithcm.model.Credential;
import edu.ptithcm.util.StorageUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class CredentialManager {
    private static final String storedKeyFileName = "chat-p2p-credential";

    public static Credential readStoredCredential(){
        return readCredentialFromFile(StorageUtils.getAppDataDirectory().resolve(storedKeyFileName));
    }

    public static Credential readCredentialFromFile(String filePath){
        return readCredentialFromFile(Path.of(filePath));
    }

    public static Credential readCredentialFromFile(Path filePath){
        if (!Files.exists(filePath))
            return null;
        try{
            List<String> lines = Files.readAllLines(filePath);
            if(lines.size() < 3)
                return null;

            PublicKey publicKey = CryptoUtils.stringToPublicKey(lines.get(0));
            PrivateKey privateKey = CryptoUtils.stringToPrivateKey(lines.get(1));
            String name = lines.get(2);
            if(publicKey == null || privateKey == null || name == null)
                return null;
            return new Credential(publicKey, privateKey, name);
        } catch (Exception e) {
            return null;
        }
    }

    //write to default file
    public static void writeCredentialToFile(Credential credential){
        List<String> lines = new ArrayList<>();
        lines.add(CryptoUtils.publicKeyToString(credential.getPublicKey()));
        lines.add(CryptoUtils.privateKeyToString(credential.getPrivateKey()));
        lines.add(credential.getName());

        Path dir = StorageUtils.getAppDataDirectory();
        Path filePath = dir.resolve(storedKeyFileName);
        try{
            Files.createDirectories(dir);
            Files.write(
                    filePath,
                    lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            return;
        }
    }

    static void main() {
//        IO.println("Test KeyManager");
////        KeyPair kp = getStoredKeyPair();
////        IO.println((kp == null));
//
//        KeyPair kp = CryptoUtils.generateRSAKeyPair();
//        writeKeyPairToFile(kp);
//
//        KeyPair kp2 = getStoredKeyPair();
//        if(kp2 == null){
//            IO.println("SOS, something wrong :((");
//            System.exit(1);
//        }
//
//        IO.println("original");
//        IO.println(CryptoUtils.publicKeyToString(kp.getPublic()) + "\n" + CryptoUtils.privateKeyToString(kp.getPrivate()));
//
//        IO.println("original");
//        IO.println(CryptoUtils.publicKeyToString(kp2.getPublic()) + "\n" + CryptoUtils.privateKeyToString(kp2.getPrivate()));
    }
}
