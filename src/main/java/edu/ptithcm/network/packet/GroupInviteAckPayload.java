package edu.ptithcm.network.packet;

import edu.ptithcm.security.Signable;

public class GroupInviteAckPayload implements Signable {
    private final String groupId;
    private final String senderId; // Người được mời
    private final boolean accept;
    private final long timestamp;
    private String signature;

    public GroupInviteAckPayload(String groupId, String senderId, boolean accept) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.accept = accept;
        this.timestamp = System.currentTimeMillis();
    }

    // ... Getters ...

    @Override
    public String getSignableData() {
        return groupId + senderId + accept + timestamp;
    }

    @Override
    public String getSignature() {
        return "";
    }

    @Override
    public void setSignature(String signature) {

    }
}