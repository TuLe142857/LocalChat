package edu.ptithcm.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupConversation extends Conversation{
//    private final CopyOnWriteArrayList<Peer> participants;

    private final ConcurrentHashMap<String, Peer> participants;
    /**
     * <pre>
     *     Create an empty GroupConversation
     *     Group.id == random UUID
     * </pre>
     * @param name group name
     */
    public GroupConversation(String name){
        super(name);
        this.participants = new ConcurrentHashMap<>();
    }

    public GroupConversation(String name, String id){
        super(name, id);
        this.participants = new ConcurrentHashMap<>();
    }

    public void addParticipants(Peer peer){
        this.participants.putIfAbsent(peer.getId(), peer);
    }

    public void removeParticipant(String peerID){
        this.participants.remove(peerID);
    }

    public List<Peer> getParticipantList(){
        return new ArrayList<>(participants.values());
    }

    public Peer getParticipant(String peerId){
        return participants.get(peerId);
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
