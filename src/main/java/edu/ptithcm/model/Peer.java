package edu.ptithcm.model;


import java.net.InetAddress;
import java.security.PublicKey;

public class Peer {
    private final String id;
    private final PublicKey publicKey;
    private String name;
    private InetAddress ip;
    private int port;

    public Peer(String id, PublicKey publicKey, String name, InetAddress ip, int port) {
        this.id = id;
        this.publicKey = publicKey;
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public Peer(Credential credential, InetAddress ip, int port){
        this.id = credential.getId();
        this.publicKey = credential.getPublicKey();
        this.name = credential.getName();
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
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
