package edu.ptithcm.service;


import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Credential;
import edu.ptithcm.util.JsonUtils;

import java.net.InetAddress;

public class AuthService {
    public static void login(Credential credential, InetAddress ip, int port){
        IO.println("Login call");
        IO.println(JsonUtils.toJson(credential));
        IO.println(ip);
        IO.println(port);

        Cache.getInstance().setCredential(credential);
        Cache.getInstance().setIp(ip);
        Cache.getInstance().setPort(port);
    }
}
