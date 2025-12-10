package edu.ptithcm.model;


import java.net.InetAddress;
import java.security.PublicKey;

public class Peer {
    private final String id;
    private final PublicKey publicKey;
    private String name;
    private InetAddress ip;
    private String port;

    public Peer(String id, PublicKey publicKey, String name, InetAddress ip, String port) {
        this.id = id;
        this.publicKey = publicKey;
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public String getId() {
        return id;
    }


    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InetAddress getIp() {
        return ip;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "id='" + id + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}
