package edu.ptithcm.cache;

import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.Credential;
import edu.ptithcm.model.Peer;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private static final Cache instance = new Cache();
    // credential & ip, port
    private Credential credential;
    private InetAddress ip;
    private int port; // tcp listening port

    // chat cache
    private final ConcurrentHashMap<String, Peer> knownPeers;
    private final ConcurrentHashMap<String, Conversation> conversations;

    private Cache(){
        knownPeers = new ConcurrentHashMap<>();
        conversations = new ConcurrentHashMap<>();
    }

    public static Cache getInstance(){
        return instance;
    }

    // clear cache (use for logout, ...)
    public void clear(){
        credential = null;
        ip = null;
        port  = -1;
        knownPeers.clear();
        conversations.clear();
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
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

    public void addPeer(Peer peer){
        this.knownPeers.put(peer.getId(), peer);
    }

    public void addConversation(Conversation conversation){
        this.conversations.put(conversation.getId(), conversation);
    }

    public Peer getPeer(String id){
        return this.knownPeers.get(id);
    }

    public Conversation getConversation(String id){
        return this.conversations.get(id);
    }
}
