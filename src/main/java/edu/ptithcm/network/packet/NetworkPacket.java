package edu.ptithcm.network.packet;

import edu.ptithcm.util.JsonUtils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

/**
 * NetworkPacket: data to transfer on network
 * NetworkPacket.PacketType:
 *
 */
public class NetworkPacket {
    public static enum PacketType{
        DISCOVER,

        HANDSHAKE,
        HANDSHAKE_ACK,

        MESSAGE,
        MESSAGE_ACK,

        GROUP_INVITE,
        GROUP_INVITE_ACK,
        GROUP_UPDATE,

        SYNC_METADATA_REQUEST, //payload empty
        SYNC_METADATA_RESPONSE,

        FETCH_MESSAGE_REQUEST,
        FETCH_MESSAGE_RESPONSE,

        HEART_BEAT //payload empty
    }
    private final PacketType packetType;
    private final String payload;


    public NetworkPacket(PacketType packetType, String payload) {
        this.packetType = packetType;
        this.payload = payload;
    }

    public byte[] toBytes(){
        return JsonUtils.toJson(this).getBytes();
    }

    public static NetworkPacket fromBytes(byte[] buf, int start, int end){
        String json = new String(buf, start, end, StandardCharsets.UTF_8);
        return JsonUtils.fromJson(json, NetworkPacket.class);
    }

    public static NetworkPacket fromDatagramPacket(DatagramPacket datagramPacket){
        return fromBytes(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());
    }

    public <T> T getPayloadAs(Class<T> clazz){
        return JsonUtils.fromJson(this.payload, clazz);
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public String getPayload() {
        return payload;
    }
}
