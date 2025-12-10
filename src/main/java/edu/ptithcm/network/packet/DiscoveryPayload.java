package edu.ptithcm.network.packet;

import edu.ptithcm.model.Peer;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.security.Signable;


public class DiscoveryPayload implements Signable {
    private final Peer peer;
    private final long timestamp;

    private String signature;

    public DiscoveryPayload(Peer peer) {
        this.peer = peer;
        this.timestamp = System.currentTimeMillis();
    }

    public Peer getPeer() {
        return peer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignableData() {
        return peer.getId()
                + CryptoUtils.publicKeyToString(peer.getPublicKey())
                + peer.getName()
                + peer.getIp()
                + peer.getPort()
                +timestamp;
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
