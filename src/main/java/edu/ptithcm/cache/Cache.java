package edu.ptithcm.cache;

import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.Credential;
import edu.ptithcm.model.Peer;

import java.net.InetAddress;
import java.util.*;
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
    private final ConcurrentHashMap<String, Set<String>> pendingInviteGroupMember;

    private Cache(){
        knownPeers = new ConcurrentHashMap<>();
        conversations = new ConcurrentHashMap<>();
        pendingInviteGroupMember = new ConcurrentHashMap<>();
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
        this.knownPeers.putIfAbsent(peer.getId(), peer);
    }

    public void addConversation(Conversation conversation){
        this.conversations.putIfAbsent(conversation.getId(), conversation);
    }

    public boolean removeConversation(String id){
        return this.conversations.remove(id) != null;
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
    public List<Conversation> getConversationList(){
        return new ArrayList<>(this.conversations.values());
    }

    public void addPendingGroupInvite(String groupId, String peerId){
        this.pendingInviteGroupMember.computeIfAbsent(
                groupId, k->ConcurrentHashMap.newKeySet()
        ).add(peerId);
    }

    public Set<String> getPendingGroupInvite(String groupId) {
        return pendingInviteGroupMember.get(groupId);
    }

    public boolean removePendingGroupInvite(String groupId, String peerId) {
        Set<String> set = pendingInviteGroupMember.get(groupId);
        if (set == null){
            return false;
        }

        boolean removed = set.remove(peerId);

        if (set.isEmpty()) {
            pendingInviteGroupMember.remove(groupId, set);
        }
        return removed;
    }

}
