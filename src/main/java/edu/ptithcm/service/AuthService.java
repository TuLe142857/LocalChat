package edu.ptithcm.service;


import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Credential;
import edu.ptithcm.util.JsonUtils;
import org.tinylog.Logger;

import java.net.InetAddress;

public class AuthService {
    public static void login(Credential credential, InetAddress ip, int port){
        Logger.info(String.format("Login call:\n\tcredential: %s\n\tip: %s\n\tport: %d", JsonUtils.toJson(credential), ip.toString(), port));

        Cache.getInstance().setCredential(credential);
        Cache.getInstance().setIp(ip);
        Cache.getInstance().setPort(port);
    }
}
