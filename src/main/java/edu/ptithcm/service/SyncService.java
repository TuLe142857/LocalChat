package edu.ptithcm.service;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.*;
import edu.ptithcm.network.core.ConnectionPool;
import edu.ptithcm.network.packet.*;
import edu.ptithcm.util.JsonUtils;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SyncService {

    /**
     * Subscribe MessageBus
     */
    public static void init(){

    }


    public static void requestSyncMetadata(Peer targetPeer){
        Logger.debug("Send request sync metadata to peer " + targetPeer.getId());
        ConnectionPool.getInstance().getOrConnect(targetPeer)
                .thenAccept(
                        peerConnection -> {
                            SyncMetadataRequestPayload payload = new SyncMetadataRequestPayload(Cache.getInstance().getCredential().getId());
                            //sign
                            payload.sign(Cache.getInstance().getCredential().getPrivateKey());
                            NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.SYNC_METADATA_REQUEST, JsonUtils.toJson(payload));
                            try {
                                peerConnection.sendNetworkPacket(networkPacket);
                            } catch (IOException e) {
//                                throw new RuntimeException(e);
                                Logger.warn("Request syns metadata failed(connection error); targetPeerId = " + targetPeer.getId());
                            }
                        }
                );
    }

    public static void requestFetchMessageDirectConversation(String conversationId, long clockBefore, long messageLimit){
        Conversation conv = Cache.getInstance().getConversation(conversationId);
        if((conv == null) || (!(conv instanceof DirectConversation))){
            Logger.error("conversation not exit or is not instance of direct chat");
            return;
        }
        DirectConversation dConv = (DirectConversation)(conv);

        FetchMessageRequestPayload payload = new FetchMessageRequestPayload(
                Cache.getInstance().getCredential().getId(),
                conversationId,
                clockBefore,
                messageLimit
        );
        payload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.FETCH_MESSAGE_REQUEST, JsonUtils.toJson(payload));

        ConnectionPool.getInstance().getOrConnect(dConv.getPartner())
                .thenAccept(
                        peerConnection -> {
                            try{
                                peerConnection.sendNetworkPacket(networkPacket);
                            }catch (IOException e){
                                Logger.error("Send FetchMessageDirectChat failed: connection error");
                            }
                        }
                );

    }

    public static void requestFetchMessageGroupConversation(String groupId, String fetchPeerId, long clockBefore, long messageLimit){
        Conversation conversation = Cache.getInstance().getConversation(groupId);
        if((conversation == null) || (!(conversation instanceof GroupConversation))){
            return;

        }
        Peer fetchPeer = Cache.getInstance().getPeer(fetchPeerId);
        if(fetchPeer == null){
            return;
        }

        FetchMessageRequestPayload payload = new FetchMessageRequestPayload(
                Cache.getInstance().getCredential().getId(),
                groupId,
                clockBefore,
                messageLimit
        );
        payload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.FETCH_MESSAGE_REQUEST, JsonUtils.toJson(payload));
        ConnectionPool.getInstance().getOrConnect(fetchPeer)
                .thenAccept(
                        peerConnection -> {
                            try{
                                peerConnection.sendNetworkPacket(networkPacket);
                            }catch (IOException e){
                                Logger.error("Send FetchMessageGroupChat failed: connection error");
                            }
                        }
                );

    }

    public static void handleSyncMetadataRequest(SyncMetadataRequestPayload request){
        Logger.debug("Handle sync metadata request from peer" + request.getSenderId());
        // verify ...
        Peer targetPeer = Cache.getInstance().getPeer(request.getSenderId());
        if(targetPeer == null){
            Logger.error("Handle request sync metadata but targetPeer is null, id = " + request.getSenderId());
            return;
        }
        if(!request.verify(targetPeer.getPublicKey())){
            Logger.error("Verify signature of SyncMetadataRequestPayload failed, peerID: " + request.getSenderId());
            return;
        }

        // find data
        Conversation directConversation = Cache.getInstance().getConversation(targetPeer.getId());
        long directChatClock = (directConversation != null) ? (directConversation.getLamportClock()) : (0);
        List<SyncMetadataResponsePayload.GroupConversationInfo> groupConversationInfoList = new ArrayList<>();
        for(var conv : Cache.getInstance().getConversationList()){
            if (! (conv instanceof GroupConversation))
                continue;

            // check
            for (var participant : ((GroupConversation) conv).getParticipantList()){
                if(participant.getId().equals(request.getSenderId())){
                    groupConversationInfoList.add(
                            new SyncMetadataResponsePayload.GroupConversationInfo(
                                    conv.getId(),
                                    conv.getName(),
                                    conv.getLamportClock(),
                                    ((GroupConversation) conv).getParticipantList()
                            )
                    );
                    break;
                }
            }
        }

        // make network packet
        SyncMetadataResponsePayload payload = new SyncMetadataResponsePayload(
                Cache.getInstance().getCredential().getId(),
                directChatClock,
                groupConversationInfoList
        );
        payload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.SYNC_METADATA_RESPONSE, JsonUtils.toJson(payload));


        // send reply
        ConnectionPool.getInstance().getOrConnect(targetPeer)
                .thenAccept(
                        peerConnection -> {
                            try{
                                peerConnection.sendNetworkPacket(networkPacket);
                            }catch (Exception e){
                                Logger.warn("Send response sync metadata fata failed to peer " + request.getSenderId() +" : connection error");
                            }
                        }
                );
    }

    public static void handleSyncMetadataResponse(SyncMetadataResponsePayload responsePayload){
        Peer senderPeer = Cache.getInstance().getPeer(responsePayload.getSenderId());
        if(senderPeer == null){
            return;
        }

        if(!responsePayload.verify(senderPeer.getPublicKey())){
            Logger.error("Verify signature failed");
            return;
        }

        // sync direct chat
        if(
                (Cache.getInstance().getConversation(responsePayload.getSenderId()) == null)
                && (responsePayload.getDirectChatClock() > 0))
        {
            DirectConversation dConv = new DirectConversation(senderPeer);
            dConv.setLamportClock(responsePayload.getDirectChatClock());

            Cache.getInstance().addConversation(dConv);

            requestFetchMessageDirectConversation(senderPeer.getId(), responsePayload.getDirectChatClock(), 50);
        }


//         group chat
        for(var groupInfo : responsePayload.getGroupConversationInfoList()){
            if(Cache.getInstance().getConversation(groupInfo.getGroupId()) == null){
                GroupConversation groupConversation = new GroupConversation(groupInfo.getGroupName(), groupInfo.getGroupId());
                Cache.getInstance().addConversation(groupConversation);
            }

            GroupConversation groupConversation = (GroupConversation) (Cache.getInstance().getConversation(groupInfo.getGroupId()));

            // update clock
            groupConversation.setLamportClock(Math.max(groupConversation.getLamportClock(), groupInfo.getClock()));

            //update participants
            for(var participant : groupInfo.getParticipants()){
                // Chỉ dùng Peer trong cached
                Peer cachedPeer = Cache.getInstance().getPeer(participant.getId());
                if(cachedPeer == null){
                    Cache.getInstance().addPeer(participant);
                    cachedPeer = participant;
                }

                // this function will check existing before add
                groupConversation.addParticipants(cachedPeer);
            }

            // request fetch data
            long minLamportClock = groupConversation.getMessageList().getFirst().getLamportClock();
            requestFetchMessageGroupConversation(groupConversation.getId(), senderPeer.getId(), minLamportClock, 50);
        }
    }

    public static void handleFetchMessageRequest(FetchMessageRequestPayload request){
        Peer senderPeer = Cache.getInstance().getPeer(request.getSenderId());
        if(senderPeer == null || (!request.verify(senderPeer.getPublicKey()))){
            return;
        }

        boolean isDirectChat = request.getConversationId().equals(Cache.getInstance().getCredential().getId());

        List<Message> responseMessageList = new ArrayList<>();
        if(isDirectChat){
            Conversation conv = Cache.getInstance().getConversation(senderPeer.getId());
            if(conv == null){
                return;
            }
            List<Message> allMessage = conv.getSuccessMessage();
            int count = 0;
            int size = allMessage.size();
            for(int i = size-1; i >= 0; i-- ){
                Message message = allMessage.get(i);
                if(message.getLamportClock() <= request.getClockBefore()){
                    responseMessageList.add(message);
                    count ++;
                    if(count >= request.getLimit())
                        break;
                }
            }
        }else{
            Logger.warn("Chưa code xong sync group :))");
        }

        FetchMessageResponsePayload responsePayload = new FetchMessageResponsePayload(
                Cache.getInstance().getCredential().getId(),
                (isDirectChat)?(senderPeer.getId()):(request.getConversationId()),
                responseMessageList);
        responsePayload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.FETCH_MESSAGE_RESPONSE, JsonUtils.toJson(responsePayload));

        // send
        ConnectionPool.getInstance().getOrConnect(senderPeer)
                .thenAccept(
                        peerConnection -> {
                            try{
                                peerConnection.sendNetworkPacket(networkPacket);
                            }catch (IOException e){
                                Logger.warn("Connection error");
                            }
                        }
                );

    }

    public static void handleFetchMessageResponse(FetchMessageResponsePayload response){
        Peer senderPeer = Cache.getInstance().getPeer(response.getSenderId());
        if(senderPeer == null || (!response.verify(senderPeer.getPublicKey()))){
            return;
        }

        boolean isDirectChat = response.getConversationId().equals(Cache.getInstance().getCredential().getId());

        if(isDirectChat){
            Conversation conv = Cache.getInstance().getConversation(response.getSenderId());
            if(response.getMessages().isEmpty()){
                return;
            }
            Logger.debug("Get sync direct chat from: " + response.getSenderId() +" num of message = " + response.getMessages().size());
            for(var message:response.getMessages()){
                conv.onReceiveMessage(message);
            }
            Message firstMessage = conv.getSuccessMessage().getFirst();
            if(firstMessage != null && firstMessage.getLamportClock() > 1){
                requestFetchMessageDirectConversation(conv.getId(), firstMessage.getLamportClock(), 50);
            }
        }
        else {
            Logger.warn("Chua code sync group");
        }
    }
}
