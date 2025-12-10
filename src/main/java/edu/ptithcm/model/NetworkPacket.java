package edu.ptithcm.model;

import edu.ptithcm.util.JsonUtils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * NetworkPacket: data to transfer on network
 * NetworkPacket.PacketType:
 *
 */
public class NetworkPacket {
    public static enum PacketType{
        DISCOVER,
        DISCOVER_ACK,
        HANDSHAKE,
        HANDSHAKE_ACK,
        MESSAGE,
        MESSAGE_ACK,
        SYNC,
        SYNC_ACK,
        HEART_BEAT
    }
    private final PacketType packetType;
    private final String senderId;
    private final String payload;
    private final String signature;
    private final long  timestamp = Instant.now().getEpochSecond();

    public NetworkPacket(PacketType packetType, String senderId, String payload, String signature) {
        this.packetType = packetType;
        this.senderId = senderId;
        this.payload = payload;
        this.signature = signature;
    }

    public byte[] toBytes(){
        return JsonUtils.toJson(this).getBytes();
    }

    public static NetworkPacket fromBytes(byte[] buf, int start, int end){
        String json = new String(buf, start, end, StandardCharsets.UTF_8);
        return JsonUtils.fromJson(json, NetworkPacket.class);
    }

    public static NetworkPacket fromDatagramPacket(DatagramPacket datagramPacket){
        return fromBytes(datagramPacket.getData(), 0, datagramPacket.getLength());
    }


    public PacketType getPacketType() {
        return packetType;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getPayload() {
        return payload;
    }

    public String getSignature() {
        return signature;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
