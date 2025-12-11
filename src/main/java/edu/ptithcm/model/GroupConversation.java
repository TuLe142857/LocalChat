package edu.ptithcm.model;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupConversation extends Conversation{
    private final CopyOnWriteArrayList<Peer> participants;

    /**
     * <pre>
     *     Create an empty GroupConversation
     *     Group.id == random UUID
     * </pre>
     * @param name group name
     */
    public GroupConversation(String name){
        super(name);
        this.participants = new CopyOnWriteArrayList<>();
    }

    public void addParticipants(Peer peer){
        this.participants.add(peer);
    }

    public List<Peer> getParticipantList(){
        return participants;
    }

}
