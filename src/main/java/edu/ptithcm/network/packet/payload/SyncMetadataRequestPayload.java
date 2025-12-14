package edu.ptithcm.network.packet.payload;

import edu.ptithcm.security.Signable;

public class SyncMetadataRequestPayload implements Signable {
    private final String senderId;

    private final long timestamp;
    private String signature;

    public SyncMetadataRequestPayload(String senderId) {
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignableData() {
        return senderId + timestamp;
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
