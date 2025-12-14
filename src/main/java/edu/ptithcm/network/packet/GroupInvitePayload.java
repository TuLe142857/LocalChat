package edu.ptithcm.network.packet;

import edu.ptithcm.model.Peer;
import edu.ptithcm.security.Signable;
import java.util.List;

/**
 * Only Group Owner can invite member
 */
public class GroupInvitePayload implements Signable {
    private final String senderId;
    private final String groupId;
    private final String groupName;
    private final long timestamp;
    private String signature;

    public GroupInvitePayload(String senderId, String groupId, String groupName) {
        this.senderId = senderId;
        this.groupId = groupId;
        this.groupName = groupName;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignableData() {
        return senderId + groupId + groupName + timestamp;
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