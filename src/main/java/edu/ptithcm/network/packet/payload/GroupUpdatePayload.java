package edu.ptithcm.network.packet.payload;

import edu.ptithcm.model.Peer;
import edu.ptithcm.security.Signable;

/**
 * Action           Sender-Role
 * ADD_MEMBER:      Owner
 * REMOVE_MEMBER    Owner can remove anyone, member can leave
 * CHANGE_OWNER     Owner
 */
public class GroupUpdatePayload implements Signable {
    public enum Action {
        ADD_MEMBER,
        LEAVE_GROUP,
    }

    private final String senderId;
    private final String groupId;
    private final Action action;
    private final Peer targetPeer; // Đối tượng bị tác động (người mới, người leave(senderid == targetPeer.id))
    private final long timestamp;
    private String signature;

    public GroupUpdatePayload(String senderId, String groupId, Action action, Peer targetPeer) {
        this.senderId = senderId;
        this.groupId = groupId;
        this.action = action;
        this.targetPeer = targetPeer;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public String getGroupId() {
        return groupId;
    }

    public Action getAction() {
        return action;
    }

    public Peer getTargetPeer() {
        return targetPeer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignableData() {
        return senderId + groupId + action.toString() + targetPeer.getId() + timestamp;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature;
    }
}