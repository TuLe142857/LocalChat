package edu.ptithcm.network.packet;

import edu.ptithcm.security.Signable;

public class HandshakeAckPayload implements Signable {
    private final boolean accept;
    private final long timestamp;

    private String signature;

    public HandshakeAckPayload(boolean accept){
        this.accept = accept;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isAccept() {
        return accept;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignableData() {
        return String.valueOf(accept) + timestamp;
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
