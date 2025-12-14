package edu.ptithcm.cache;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.NewConversationEvent;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.Credential;
import edu.ptithcm.model.Message;
import edu.ptithcm.model.Peer;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private static final Cache instance = new Cache();

    // USER PROFILE(credential & ip, port)
    private Credential credential;
    private InetAddress ip;
    private int port; // tcp listening port

    // CHAT CACHE
    private final ConcurrentHashMap<String, Peer> knownPeers;
    private final ConcurrentHashMap<String, Conversation> conversations;
    private final ConcurrentHashMap<String, Set<String>> pendingInviteGroupMember;
    private final ConcurrentHashMap<String, Message> pendingMessage;

    private Cache(){
        knownPeers = new ConcurrentHashMap<>();
        conversations = new ConcurrentHashMap<>();
        pendingInviteGroupMember = new ConcurrentHashMap<>();
        pendingMessage = new ConcurrentHashMap<>();
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
        pendingInviteGroupMember.clear();
        pendingMessage.clear();
    }

    /*====================================================================
                            USER PROFILE
     =====================================================================*/

    public Credential getCredential() {
        return credential;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public Peer getMyPeer(){
        return new Peer(credential, ip, port);
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /*====================================================================
                CHAT(CONVERSATION, MESSAGE, STATUS, ...)
     =====================================================================*/

    /*------------------------------------------
        PEER IN NETWORK(NOT INCLUDE SELF PEER)
    -------------------------------------------*/

    public Peer getPeer(String id){
        return this.knownPeers.get(id);
    }

    public void addPeer(Peer peer){
        this.knownPeers.putIfAbsent(peer.getId(), peer);
    }

    public List<Peer> getPeerList(){
        return new ArrayList<>(this.knownPeers.values());
    }

    public Set<Map.Entry<String, Peer>> getPeerEntrySet(){
        return knownPeers.entrySet();
    }

    /*------------------------------------------
        CONVERSATION
    -------------------------------------------*/

    public Conversation getConversation(String id){
        return this.conversations.get(id);
    }

    public void addConversation(Conversation conversation){
        if(this.conversations.putIfAbsent(conversation.getId(), conversation) == null){
            MessageBus.emit(new NewConversationEvent(conversation.getId()));
        }
    }

    public boolean removeConversation(String id){
        return this.conversations.remove(id) != null;
    }

    public List<Conversation> getConversationList(){
        return new ArrayList<>(this.conversations.values());
    }

    /*------------------------------------------
        PENDING INVITE GROUP MEMBER (WAITING FOR REPLY ACK)
        - Use on ChatService
    -------------------------------------------*/

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

    /*------------------------------------------
        PENDING MESSAGE LIST(WAIT FOR GET REPLY ACK)
        - Use in ChatService
    -------------------------------------------*/

    public List<Message> getPendingMessageList(){
        return new ArrayList<>(this.pendingMessage.values());
    }

    public void addPendingMessage(Message message){
        this.pendingMessage.putIfAbsent(message.getId(), message);
    }

    public boolean removePendingMessage(String messageId){
        return (this.pendingMessage.remove(messageId) != null);
    }
}
