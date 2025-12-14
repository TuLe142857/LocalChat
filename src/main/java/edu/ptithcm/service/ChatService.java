package edu.ptithcm.service;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendFailedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.*;
import edu.ptithcm.network.connection.ConnectionPool;
import edu.ptithcm.network.packet.*;
import edu.ptithcm.network.packet.payload.GroupInviteAckPayload;
import edu.ptithcm.network.packet.payload.GroupInvitePayload;
import edu.ptithcm.network.packet.payload.GroupUpdatePayload;
import edu.ptithcm.network.packet.payload.SyncMetadataResponsePayload;
import edu.ptithcm.util.JsonUtils;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class ChatService {
    private static final long PENDING_MESSAGE_TIMEOUT_MS = 10000;
    private static final ScheduledExecutorService scheduledExecutorService;
    static {
        Logger.debug("Start ChatService scheduled checking pending message");
        scheduledExecutorService = Executors.newScheduledThreadPool(
                1,
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                }
        );

        //start scheduler
        // check every 5 seconds
        scheduledExecutorService.scheduleWithFixedDelay(ChatService::runCheckPendingMessageTimeout, 0, 5, TimeUnit.SECONDS);
    }
    public static void init(){

    }

    /*====================================================================
                    SEND MESSAGE (DIRECT & GROUP CONVERSATION)
     ====================================================================*/
    /**
     * Bắt buộc phải tồn tại conversation, nếu không thì hàm này không tự tạo
     * @param message
     */
    public static void sendMessage(Message message){
        Conversation conversation = Cache.getInstance().getConversation(message.getConversationId());
        if(conversation == null)
            return;

        Cache.getInstance().addPendingMessage(message);
        if (conversation instanceof DirectConversation){
            DirectConversation dConversation = (DirectConversation)(conversation);
            Peer targetPeer = dConversation.getPartner();
            ConnectionPool.getInstance().getOrConnect(targetPeer)
                .thenAccept(peerConnection -> {
                    NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.MESSAGE, JsonUtils.toJson(message));
                    try{
                        peerConnection.sendNetworkPacket(networkPacket);
                    }catch (Exception e){
                        ChatService.onSendFailedMessage(message.getId(), message.getConversationId());
                    }
                })
                .exceptionally(
                        t ->{
                            ChatService.onSendFailedMessage(message.getId(), message.getConversationId());
                            return  null;
                        }
                );
        }
        else if(conversation instanceof GroupConversation){
            GroupConversation gConversation = (GroupConversation)(conversation);
            NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.MESSAGE, JsonUtils.toJson(message));
            for(Peer targetPeer: gConversation.getParticipantList()){

                // bỏ qua bản thân
                if (targetPeer.getId().equals(Cache.getInstance().getCredential().getId())) continue;

                ConnectionPool.getInstance().getOrConnect(targetPeer)
                    .thenAccept(
                        peerConnection -> {
                            try{
                                peerConnection.sendNetworkPacket(networkPacket);

                            }catch (Exception e){}
                        }
                )
                .exceptionally(
                        t->{
                            return  null;
                        }
                    );
            }
        }
    }

    /*====================================================================
                            GROUP CONVERSATION
     ====================================================================*/

    /**
     * Tạo và mời các thành viên vào nhóm
     * Mặc định sẽ tự add bản thân vào nhóm, không cần include trong selected Peer
     * Trả về GroupConversation
     * Group đã được thêm vào cache trong hàm này, không cần thêm thủ công
     * sle
     * @param groupName
     */
    public static GroupConversation createGroupConversation(String groupName, List<String> selectedPeerId){
        GroupConversation groupConversation = new GroupConversation(groupName);
        groupConversation.addParticipants(Cache.getInstance().getMyPeer());

        // add cache
        Cache.getInstance().addConversation(groupConversation);

        // send invite
        for(String peerId : selectedPeerId){
            invitePeerToGroup(groupConversation.getId(), peerId);
        }

        return groupConversation;
    }

    /**
     * Mời thành viên mới vào nhóm
     * @param groupId
     * @param peerId
     */
    public static void invitePeerToGroup(String groupId, String peerId){
        Conversation conversation = Cache.getInstance().getConversation(groupId);
        Peer targetPeer = Cache.getInstance().getPeer(peerId);

        if(conversation == null || targetPeer == null){
            Logger.error("Invite peer to group failed: group or peer is null");
            return;
        }

        if(!(conversation instanceof GroupConversation)){
            Logger.error("Invite peer to group failed: group is not instance of GroupConversation");
            return;
        }

        // add to invited list
        Cache.getInstance().addPendingGroupInvite(groupId, peerId);


        GroupConversation groupConversation = (GroupConversation)(conversation);
        GroupInvitePayload payload = new GroupInvitePayload(Cache.getInstance().getCredential().getId(), groupId, groupConversation.getName());
        payload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.GROUP_INVITE, JsonUtils.toJson(payload));

        // send invite
        ConnectionPool.getInstance().getOrConnect(targetPeer)
            .thenAccept(
                peerConnection -> {
                    try {
                        peerConnection.sendNetworkPacket(networkPacket);
                    }catch (IOException e){
                        Logger.error("Error send GROUP INVITE: connection error");
                    }
                }
            );
    }

    /**
     * Rời nhóm mà bản thân đang tham gia
     */
    public static void leaveGroup(String groupId){
        Conversation conversation = Cache.getInstance().getConversation(groupId);
        if(!(conversation instanceof GroupConversation)){
            Logger.error("Leave group error: group is null or not instance of GroupConversation");
            return;
        }

        GroupUpdatePayload payload = new GroupUpdatePayload(
                Cache.getInstance().getCredential().getId(),
                groupId,
                GroupUpdatePayload.Action.LEAVE_GROUP,
                Cache.getInstance().getMyPeer()
        );
        payload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket packet = new NetworkPacket(NetworkPacket.PacketType.GROUP_UPDATE, JsonUtils.toJson(payload));

        GroupConversation group = (GroupConversation) (conversation);
        for(Peer p : group.getParticipantList()){
            // skip self peer
            if(p.getId().equals(Cache.getInstance().getCredential().getId()))
                continue;
            ConnectionPool.getInstance().getOrConnect(p)
            .thenAccept(
                    peerConnection -> {
                        try {
                            peerConnection.sendNetworkPacket(packet);
                        } catch (IOException e) {
                            Logger.error("Send group leave failed: connection error");
                        }
                    }
                );
        }

//        group.removeParticipant(Cache.getInstance().getCredential().getId());
        Cache.getInstance().removeConversation(group.getId());
    }

    /*====================================================================
     THE FOLLOWING METHOD IS FOR NETWORK LAYER ONLY(class PeerConnection.listen())
     DO NOT CALL/HANDLE IN UI THREAD
     ====================================================================*/

    /**
     * Xử lý nhận lời mời join group: auto accept + gởi ack reply
     */
    public static void handleGroupInvite(GroupInvitePayload invitePayload){
        Peer senderPeer = Cache.getInstance().getPeer(invitePayload.getSenderId());
        if(senderPeer == null || (!invitePayload.verify(senderPeer.getPublicKey()))){
            Logger.warn("Handle GroupInvite got null senderPeer or invalid signature");
            return;
        }

        // tạo group nhưng chưa add vào cache
        GroupConversation groupConversation = new GroupConversation(invitePayload.getGroupName(), invitePayload.getGroupId());
        groupConversation.addParticipants(senderPeer);
        groupConversation.addParticipants(Cache.getInstance().getMyPeer());

        GroupInviteAckPayload responsePayload = new GroupInviteAckPayload(groupConversation.getId(), Cache.getInstance().getCredential().getId(), true);
        responsePayload.sign(Cache.getInstance().getCredential().getPrivateKey());

        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.GROUP_INVITE_ACK, JsonUtils.toJson(responsePayload));
        ConnectionPool.getInstance().getOrConnect(senderPeer)
                .thenAccept(
                        peerConnection -> {
                            try{
                                peerConnection.sendNetworkPacket(networkPacket);

                                // Chỉ add group vào cache khi gởi gói tin ack success
                                Cache.getInstance().addConversation(groupConversation);
                            }catch (IOException e){
                                Logger.error("Error send GROUP INVITE ACK: connection error");
                            }
                        }
                );
    }

    /**
     * Nhận phản hồi đồng ý join group từ peer khác (mình là người mời)
     * @param inviteAckPayload
     */
    public static void handleGroupInviteAck(GroupInviteAckPayload inviteAckPayload){
        if(!Cache.getInstance().getPendingGroupInvite(inviteAckPayload.getGroupId()).contains(inviteAckPayload.getSenderId())){
            Logger.warn("Handle GroupInviteAck: senderId was not invited to this group");
            return;
        }
        Peer senderPeer = Cache.getInstance().getPeer(inviteAckPayload.getSenderId());
        if(senderPeer == null || (!inviteAckPayload.verify(senderPeer.getPublicKey()))){
            Logger.warn("Handle GroupInviteAck got null senderPeer or invalid signature");
            return;
        }

        Conversation conversation = Cache.getInstance().getConversation(inviteAckPayload.getGroupId());
        if(!(conversation instanceof GroupConversation)){
            return;
        }

        // thêm vào nhóm
        GroupConversation groupConversation = (GroupConversation) (conversation);
        groupConversation.addParticipants(senderPeer);

        // remove from pending invite
        Cache.getInstance().removePendingGroupInvite(groupConversation.getId(), senderPeer.getId());

        // Gởi gói tin update tới các thành viên khác trong nhóm
        GroupUpdatePayload updatePayload = new GroupUpdatePayload(
                Cache.getInstance().getCredential().getId(),
                groupConversation.getId(),
                GroupUpdatePayload.Action.ADD_MEMBER,
                senderPeer
        );
        updatePayload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.GROUP_UPDATE, JsonUtils.toJson(updatePayload));
        for(Peer participant : groupConversation.getParticipantList()){
            // skip self peer
            if(participant.getId().equals(Cache.getInstance().getCredential().getId()))
                continue;
            ConnectionPool.getInstance().getOrConnect(participant)
                .thenAccept(
                    peerConnection -> {
                        try {
                            peerConnection.sendNetworkPacket(networkPacket);
                        } catch (IOException e) {
                            Logger.error("Send group update failed: connection error");
                        }
                    }
                );
        }

        // sync group cho thành viên mới
        SyncMetadataResponsePayload syncPayload = new SyncMetadataResponsePayload(
                Cache.getInstance().getCredential().getId(),0,
                SyncMetadataResponsePayload.GroupConversationInfo.getFrom(groupConversation)
        );
        syncPayload.sign(Cache.getInstance().getCredential().getPrivateKey());
        NetworkPacket syncPacket = new NetworkPacket(NetworkPacket.PacketType.SYNC_METADATA_RESPONSE, JsonUtils.toJson(syncPayload));
        ConnectionPool.getInstance().getOrConnect(senderPeer)
                .thenAccept(
                        peerConnection -> {
                            try {
                                peerConnection.sendNetworkPacket(syncPacket);
                            } catch (IOException e) {
                                Logger.error("Send sync metadata to new member of group failed: connection error");
                            }
                        }
                );
    }

    public static void handleGroupUpdatePayload(GroupUpdatePayload groupUpdatePayload){
        Peer senderPeer = Cache.getInstance().getPeer(groupUpdatePayload.getSenderId());
        Conversation conversation = Cache.getInstance().getConversation(groupUpdatePayload.getGroupId());

        if (senderPeer==null || (!(conversation instanceof GroupConversation)) || (!groupUpdatePayload.verify(senderPeer.getPublicKey()))){
            Logger.warn("GroupUpdatePayload verify failed");
            return;
        }

        GroupConversation groupConversation = (GroupConversation) (conversation);
        if(groupUpdatePayload.getAction() == GroupUpdatePayload.Action.LEAVE_GROUP){
            if(groupUpdatePayload.getSenderId().equals(groupUpdatePayload.getTargetPeer().getId())){
                groupConversation.removeParticipant(groupUpdatePayload.getSenderId());
            }
        }else if(groupUpdatePayload.getAction() == GroupUpdatePayload.Action.ADD_MEMBER){
            // lấy peer trong cache để đảm bảo đúng tham chiếu
            Peer cachedPeer = Cache.getInstance().getPeer(groupUpdatePayload.getTargetPeer().getId());

            // unknown new peer
            if(cachedPeer == null){
                Cache.getInstance().addPeer(groupUpdatePayload.getTargetPeer());
                cachedPeer = groupUpdatePayload.getTargetPeer();
            }
            groupConversation.addParticipants(cachedPeer);
        }
    }


    public static void onReceiveMessage(Message message){
        boolean isDirectChatMessage = message.getConversationId().equals(Cache.getInstance().getCredential().getId());
        String conversationId = isDirectChatMessage
                                ? (message.getSenderId())
                                : (message.getConversationId());
        Conversation conversation = Cache.getInstance().getConversation(conversationId);
        if(conversation == null){
            Peer partner = Cache.getInstance().getPeer(message.getSenderId());
            if(partner == null)
                return;

            IO.println("Create new direct conversation");
            DirectConversation dConversation = new DirectConversation(partner);
            Cache.getInstance().addConversation(dConversation);
            dConversation.onReceiveMessage(message);
        }
        else{
            conversation.onReceiveMessage(message);
        }
        MessageBus.emit(new MessageReceivedEvent(message));
    }

    public static void onSendSuccessMessage(String messageId, String conversationId){
        Conversation conversation = Cache.getInstance().getConversation(conversationId);
        if(conversation == null)
            return;
        conversation.getPendingMessage()
                .stream()
                .filter(m->(m.getId().equals(messageId)))
                .findFirst()
                .ifPresent(
                        message -> {message.setStatus(Message.MessageStatus.SUCCESS);}
                );
        Cache.getInstance().removePendingMessage(messageId);
        MessageBus.emit(new MessageSendSuccessEvent(messageId, conversationId));
    }

    public static void onSendFailedMessage(String messageId, String conversationId){
        Conversation conversation = Cache.getInstance().getConversation(conversationId);
        if(conversation == null)
            return;
        conversation.getMessageList()
                .stream()
                .filter(m->(m.getId().equals(messageId)))
                .findFirst()
                .ifPresent(
                        message -> {message.setStatus(Message.MessageStatus.FAILED);}
                );
        Cache.getInstance().removePendingMessage(messageId);
        MessageBus.emit(new MessageSendFailedEvent(messageId, conversationId));
    }

    /**
     *
     */
    private static void runCheckPendingMessageTimeout(){
        for(Message message:Cache.getInstance().getPendingMessageList()){
            if ((System.currentTimeMillis() - message.getTimestamp()) > PENDING_MESSAGE_TIMEOUT_MS){
                Logger.debug("Find 1 pending message timeout (mess.id = "+ message.getId() + " conversation.id = " + message.getConversationId());
                ChatService.onSendFailedMessage(message.getId(), message.getConversationId());
            }
        }
    }

}
