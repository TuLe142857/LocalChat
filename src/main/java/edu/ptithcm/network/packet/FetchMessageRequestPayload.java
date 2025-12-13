package edu.ptithcm.network.packet;

import edu.ptithcm.security.Signable;

/**
 * Request fetch message from conversation
 */
public class FetchMessageRequestPayload implements Signable {
    private final String senderId;
    private final String conversationId;
    private final long clockBefore;
    private final long limit;

    private final long timestamp;
    private String signature;

    public FetchMessageRequestPayload(String senderId, String conversationId, long clockBefore, long limit) {
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.clockBefore = clockBefore;
        this.limit = limit;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public long getClockBefore() {
        return clockBefore;
    }

    public long getLimit() {
        return limit;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignableData() {
        return "";
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
