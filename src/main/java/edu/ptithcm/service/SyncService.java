package edu.ptithcm.service;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.*;
import edu.ptithcm.network.connection.ConnectionPool;
import edu.ptithcm.network.packet.*;
import edu.ptithcm.network.packet.payload.FetchMessageRequestPayload;
import edu.ptithcm.network.packet.payload.FetchMessageResponsePayload;
import edu.ptithcm.network.packet.payload.SyncMetadataRequestPayload;
import edu.ptithcm.network.packet.payload.SyncMetadataResponsePayload;
import edu.ptithcm.util.JsonUtils;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 *  Sync:
 *  - Metadata Sync
 *  - Message Sync
 *
 *  Workflow example, peer A make request sync data from peer B:
 *      A [request sync] => B
 *      B [handle request sync][send response] =>A
 *      A [handle response][update data]
 * </pre>
 */
public class SyncService {

    /**
     * Subscribe MessageBus
     */
    public static void init(){

    }

    /* =====================================================================
                    METADATA SYNC
                    - CONVERSATION ID
                    - CONVERSATION LAMPORT CLOCK
                    - PARTICIPANTS LIST(GROUP CHAT)
     =======================================================================*/

    /**
     *
     * @param targetPeer
     */
    public static void requestSyncMetadata(Peer targetPeer){
        Logger.debug("Send request sync metadata to peer " + targetPeer.getId());
        ConnectionPool.getInstance().getOrConnect(targetPeer)
                .thenAccept(
                        peerConnection -> {
                            SyncMetadataRequestPayload payload = new SyncMetadataRequestPayload(Cache.getInstance().getCredential().getId());
                            payload.sign(Cache.getInstance().getCredential().getPrivateKey());
                            NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.SYNC_METADATA_REQUEST, JsonUtils.toJson(payload));
                            try {
                                peerConnection.sendNetworkPacket(networkPacket);
                            } catch (IOException e) {
                                Logger.warn("Request syns metadata failed(connection error); targetPeerId = " + targetPeer.getId());
                            }
                        }
                );
    }

    /**
     * Handle Sync Metadata Request from another Peer, check/verify and send response
     * @param request
     */
    public static void handleSyncMetadataRequest(SyncMetadataRequestPayload request){
        Logger.debug("Handle sync metadata request from peer" + request.getSenderId());
        /*------------------------------------------
                        VERIFY REQUEST
         -------------------------------------------*/
        Peer senderPeer = Cache.getInstance().getPeer(request.getSenderId());
        if(senderPeer == null){
            Logger.error("Handle request sync metadata but targetPeer is null, id = " + request.getSenderId());
            return;
        }
        if(!request.verify(senderPeer.getPublicKey())){
            Logger.error("Verify signature of SyncMetadataRequestPayload failed, peerID: " + request.getSenderId());
            return;
        }

        /*------------------------------------------
                MAKE RESPONSE PAYLOAD & SEND
         -------------------------------------------*/
        // DIRECT CONVERSATION
        Conversation directConversation = Cache.getInstance().getConversation(senderPeer.getId());
        long directChatClock = (directConversation != null) ? (directConversation.getLamportClock()) : (0);

        // GROUP CONVERSATION
        List<SyncMetadataResponsePayload.GroupConversationInfo> groupConversationInfoList = new ArrayList<>();
        for(var conv : Cache.getInstance().getConversationList()){
            if (! (conv instanceof GroupConversation))
                continue;

            // check
            if(((GroupConversation)(conv)).getParticipant(senderPeer.getId()) != null){
                groupConversationInfoList.add(
                    new SyncMetadataResponsePayload.GroupConversationInfo(
                            conv.getId(),
                            conv.getName(),
                            conv.getLamportClock(),
                            ((GroupConversation) conv).getParticipantList()
                    )
                );
            }
        }

        // SEND
        SyncMetadataResponsePayload payload = new SyncMetadataResponsePayload(
                Cache.getInstance().getCredential().getId(),
                directChatClock,
                groupConversationInfoList
        );
        payload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.SYNC_METADATA_RESPONSE, JsonUtils.toJson(payload));

        ConnectionPool.getInstance().getOrConnect(senderPeer)
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

    /**
     * Handle Sync Metadata response from another peer
     * @param responsePayload
     */
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

            // request fetch message
            List<Message> messageList = groupConversation.getMessageList();
            long minLamportClock = (messageList.isEmpty()) ? (groupConversation.getLamportClock()) : (messageList.getFirst().getLamportClock());
            requestFetchMessageGroupConversation(groupConversation.getId(), senderPeer.getId(), minLamportClock, 50);
        }
    }

    /* =====================================================================
                MESSAGE SYNC
                - DIRECT CONVERSATION MESSAGE
                - GROUP CONVERSATION MESSAGE
    =======================================================================*/

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

    public static void handleFetchMessageRequest(FetchMessageRequestPayload request){
        Peer senderPeer = Cache.getInstance().getPeer(request.getSenderId());
        if(senderPeer == null || (!request.verify(senderPeer.getPublicKey()))){
            return;
        }
        boolean isDirectChat = request.getConversationId().equals(Cache.getInstance().getCredential().getId());
        Conversation conversation;
        if(isDirectChat){
            conversation = Cache.getInstance().getConversation(senderPeer.getId());
            if(conversation == null){
                Logger.warn("Fetch Message Request for Direct Conversation: null conversation");
                return;
            }

        }else{
            // check if sender in this group
            conversation = Cache.getInstance().getConversation(request.getConversationId());
            if(!(conversation instanceof GroupConversation)){
                Logger.warn("Invalid fetch message request for group conversation: conversation is not instance of Group Conversation");
                return;
            }
            GroupConversation groupConversation = (GroupConversation) (conversation);
            if(groupConversation.getParticipant(request.getSenderId()) == null){
                Logger.warn("Invalid fetch message request for group conversation: sender do not in this group!");
                return;
            }
        }

        List<Message> responseMessageList = new ArrayList<>();
        List<Message> allMessage = conversation.getSuccessMessage();
        int count = 0;
        int size = allMessage.size();
        for(int i = size-1; i >= 0; i -- ){
            Message message = allMessage.get(i);
            if(message.getLamportClock() <= request.getClockBefore()){
                responseMessageList.add(message);
                count ++;
                if(count >= request.getLimit())
                    break;
            }
        }

        // send
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
        Conversation conversation;
        if(isDirectChat){
            conversation = Cache.getInstance().getConversation(response.getSenderId());
            Logger.debug("Get sync direct chat from: " + response.getSenderId() +" num of message = " + response.getMessages().size());
        }
        else {
            conversation = Cache.getInstance().getConversation(response.getConversationId());
            if(!(conversation instanceof GroupConversation)){
                return;
            }
            Logger.debug("Get sync group chat from: " + response.getSenderId() +" num of message = " + response.getMessages().size());
        }

        // update
        if(response.getMessages().isEmpty()){
            return;
        }
        for(var message:response.getMessages()){
            // verify signature
            Peer messageSender = Cache.getInstance().getPeer(message.getSenderId());
            if(messageSender == null){
                Logger.warn("Sync message got null sender");
                continue;
            }
            if(message.verify(messageSender.getPublicKey())){
                conversation.onReceiveMessage(message);
            }else{
                Logger.warn("Sync message verify signature failed");
                continue;
            }
        }

        // try fetch more message if possible
        Message firstMessage = conversation.getSuccessMessage().getFirst();
        if(firstMessage != null && firstMessage.getLamportClock() > 1){
            requestFetchMessageDirectConversation(conversation.getId(), firstMessage.getLamportClock()-1, 50);
        }
    }
}
