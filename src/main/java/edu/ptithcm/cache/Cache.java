package edu.ptithcm.cache;

import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.Credential;
import edu.ptithcm.model.Peer;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong; // NEW IMPORT

public class Cache {
    private static final Cache instance = new Cache();

    // credential & ip, port
    private Credential credential;
    private InetAddress ip;
    private int port; // tcp listening port

    // Lamport Clock
    private final AtomicLong lamportClock; // NEW FIELD for Lamport Clock

    // chat cache
    private final ConcurrentHashMap<String, Peer> knownPeers;
    private final ConcurrentHashMap<String, Conversation> conversations;

    private Cache(){
        knownPeers = new ConcurrentHashMap<>();
        conversations = new ConcurrentHashMap<>();
        lamportClock = new AtomicLong(0); // Initialize Lamport Clock
    }

    public static Cache getInstance(){
        return instance;
    }

    // NEW METHOD: Increment and get the new Lamport Clock value
    public long incrementLamportClock() {
        return lamportClock.incrementAndGet();
    }

    // NEW METHOD: Update Lamport Clock based on incoming message's clock
    public void updateLamportClock(long receivedClock) {
        long currentClock = lamportClock.get();
        // Cố gắng đặt giá trị lớn hơn. Nếu receivedClock > currentClock, đặt giá trị mới
        while (receivedClock > currentClock && !lamportClock.compareAndSet(currentClock, receivedClock)) {
            currentClock = lamportClock.get();
        }
        // Sau khi đã đồng bộ, increment thêm 1 (theo luật Lamport Clock)
        if (receivedClock >= currentClock) {
            lamportClock.incrementAndGet();
        }
    }

    public long getLamportClock() {
        return lamportClock.get();
    }

    // clear cache (use for logout, ...)
    public void clear(){
        credential = null;
        ip = null;
        port  = -1;
        knownPeers.clear();
        conversations.clear();
        lamportClock.set(0); // Reset clock on clear
    }

    public Peer getMyPeer(){
        return new Peer(credential, ip, port);
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
    public Set<Map.Entry<String, Peer>> getPeerEntrySet(){
        return knownPeers.entrySet();
    }
    public Conversation getConversation(String id){
        return this.conversations.get(id);
    }
    public Collection<Peer> getKnownPeersCollection(){
        return Collections.unmodifiableCollection(knownPeers.values());
    }
    public List<Conversation> getConversationList(){
        return new ArrayList<>(this.conversations.values());
    }
    // NEW METHOD: Get peer list for SearchView
    public List<Peer> getPeerList(){
        return new ArrayList<>(this.knownPeers.values());
    }
}