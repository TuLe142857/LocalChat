package edu.ptithcm.util;
import module java.base;
public class StorageUtils {
    public static final String appDataDirectoryName = "LocalChatP2P";

    public static Path getAppDataDirectory(){
        String os = System.getProperty("os.name").toLowerCase();
        String dir;
        if(os.contains("win")){
            dir = System.getenv("LOCALAPPDATA");
        }else{
            dir = System.getenv("HOME");
        }
        if(dir==null || dir.isBlank())
            dir = System.getProperty("user.dir");

        return Path.of(dir, appDataDirectoryName);
    }
}
