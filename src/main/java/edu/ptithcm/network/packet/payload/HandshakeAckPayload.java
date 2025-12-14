package edu.ptithcm.network.packet.payload;

import edu.ptithcm.security.Signable;

public class HandshakeAckPayload implements Signable {
    private final String senderId;
    private final boolean accept;
    private final long timestamp;

    private String signature;

    public HandshakeAckPayload(String senderId, boolean accept){
        this.senderId = senderId;
        this.accept = accept;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public boolean isAccept() {
        return accept;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignableData() {
        return senderId + accept + timestamp;
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
