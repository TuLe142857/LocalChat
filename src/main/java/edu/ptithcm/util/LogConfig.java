package edu.ptithcm.util;
import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

public class LogConfig {
    public static void config(){
        config(true, true);
    }
    public static void config(boolean enableConsoleLog, boolean enableFileLog){


        if(enableConsoleLog){
            Configuration.set("writer1", "console");
            Configuration.set("writer1.format", "{date: HH:mm:ss} [{thread}] {class-name}.{method} {level}:\n\t{message}");
        }

        if(enableFileLog){
            Path dir = StorageUtils.getAppDataDirectory().resolve("log");
            try{
                Files.createDirectories(dir);
            }catch (Exception e){}
            String path = dir.resolve("app-log").toString();
            IO.println("Log file save at: " + path);
            Configuration.set("writer2", "rolling file");
            Configuration.set("writer2.file", path+".log");
            Configuration.set("writer2.policies", "startup, size: 10MB");
            Configuration.set("writer2.backups", "10");
            Configuration.set("writer2.format", "{date: yyyy-MM-dd HH:mm:ss} [{thread}] {class}.{method} {level}:\n\t{message}");
        }

    }

    static void main(){
        config();
        Logger.info("hello");
        Logger.debug("hi");
        Logger.error("hi");
        Logger.warn("warn");

    }
}
