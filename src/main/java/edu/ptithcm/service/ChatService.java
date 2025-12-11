package edu.ptithcm.service;

import edu.ptithcm.model.Message;
import edu.ptithcm.model.Peer;
import edu.ptithcm.network.core.ConnectionPool;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.util.JsonUtils;

public class ChatService {

    // Send direct message or group message
    public static void sendMessage(Peer targetPeer, Message message){
        ConnectionPool.getInstance().getOrConnect(targetPeer)
                .thenAccept(peerConnection -> {
                    NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.MESSAGE, JsonUtils.toJson(message));
                    try{
                        peerConnection.sendNetworkPacket(networkPacket);

                        // emit MessageBus event...
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                })
                .exceptionally(exception ->{
                    IO.println("send message failed");
                    return null;
                });
    }
}
