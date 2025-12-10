package edu.ptithcm.view;

import edu.ptithcm.model.Credential;

import java.net.InetAddress;

@FunctionalInterface
public interface LoginCallback {
    void onLogin(Credential credential, InetAddress ip, int port);
}
