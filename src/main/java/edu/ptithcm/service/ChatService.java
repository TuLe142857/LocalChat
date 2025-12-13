package edu.ptithcm.service;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendFailedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
import edu.ptithcm.bus.event.MessageSendingEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.GroupConversation; // <-- ĐÃ THÊM IMPORT NÀY
import edu.ptithcm.model.Message;
import edu.ptithcm.model.Peer;
import edu.ptithcm.network.core.ConnectionPool;
import edu.ptithcm.network.core.PeerConnection;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.util.JsonUtils;
import org.tinylog.Logger;
import edu.ptithcm.network.packet.MessagePayload;

import java.util.List;

public class ChatService {

    private static ChatService instance;

    // SỬA: Thay đổi từ constructor public sang private để đảm bảo chỉ dùng static init
    private ChatService() {}

    // [MODIFIED]: Logic init để sử dụng instance
    public static void init(){
        if (instance == null) {
            instance = new ChatService();
        }

        // Khởi tạo các listeners cho MessageBus
        MessageBus.subscribe(
                MessageSendingEvent.class,
                messageSendingEvent -> {
                    instance.sendMessage(messageSendingEvent.getMessage());
                }
        );

        MessageBus.subscribe(
                MessageReceivedEvent.class,
                messageReceivedEvent -> {
                    instance.onReceiveMessage(messageReceivedEvent.getMessage());
                }
        );

        MessageBus.subscribe(
                MessageSendSuccessEvent.class,
                messageSendSuccessEvent -> {
                    instance.onSendSuccessMessage(messageSendSuccessEvent.getLamportClock(), messageSendSuccessEvent.getConversationId());
                }
        );

        // [REMOVING]: MessageSendFailedEvent subscribe (giữ nguyên logic gốc)
    }

    /**
     * Bắt buộc phải tồn tại conversation, nếu không thì hàm này không tự tạo
     * @param message
     */
    private void sendMessage(Message message){
        Conversation conversation = Cache.getInstance().getConversation(message.getConversationId());
        if(conversation == null) {
            Logger.warn("Attempted to send message to non-existent conversation: " + message.getConversationId());
            return;
        }

        if (conversation instanceof DirectConversation){
            DirectConversation dConversation = (DirectConversation)(conversation);
            Peer targetPeer = dConversation.getPartner();

            // NEW LOGIC: Use ConnectionPool to connect/get connection
            ConnectionPool.getInstance().getOrConnect(targetPeer)
                    .thenAccept(peerConnection -> {
                        // NEW LOGIC: Wrap Message into MessagePayload
                        MessagePayload payload = new MessagePayload(message.getConversationId(), message.getSenderId(), message.getContent(), message.getLamportClock());
                        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.MESSAGE, JsonUtils.toJson(payload));
                        try{
                            peerConnection.sendNetworkPacket(networkPacket);
                        }catch (Exception e){
                            Logger.error(e, "Failed to send message over existing connection to " + targetPeer.getName());
                            MessageBus.emit(new MessageSendFailedEvent(message.getId(), message.getConversationId()));
                        }
                    })
                    .exceptionally(
                            t ->{
                                Logger.error(t, "Failed to establish connection for sending message to " + targetPeer.getName());
                                MessageBus.emit(new MessageSendFailedEvent(message.getId(), message.getConversationId()));
                                return  null;
                            }
                    );
        }else if(conversation instanceof GroupConversation){
            // [TODO]: Logic for Group Chat
            Logger.warn("Group conversation sending not yet implemented.");
        }
    }

    private void onReceiveMessage(Message message){
        Cache cache = Cache.getInstance();

        // Host 2: Conversation ID của tin nhắn đến là ID của Peer gửi.
        String conversationId = message.getConversationId(); // Đã được set là Peer ID của đối tác trong PeerConnection.java

        Conversation conversation = cache.getConversation(conversationId);

        // [NEW LOGIC]: Tự động tạo Direct Conversation nếu chưa có (Host 2)
        if(conversation == null){
            Peer partner = cache.getPeer(message.getSenderId());
            if(partner == null) {
                Logger.warn("Received message from unknown peer, cannot create conversation: " + message.getSenderId());
                return;
            }

            Logger.info("Create new direct conversation for incoming message from: " + partner.getName());
            DirectConversation dConversation = new DirectConversation(partner);
            cache.addConversation(dConversation);

            // Cập nhật Lamport Clock toàn cục và thêm tin nhắn vào Conversation
            cache.updateLamportClock(message.getLamportClock());
            dConversation.addMessage(message);

            // MessageReceivedEvent đã được emit trong PeerConnection, logic UI sẽ tự động xử lý.
            return;
        }

        // Nếu Conversation đã tồn tại, cập nhật Lamport Clock toàn cục và thêm tin nhắn vào
        cache.updateLamportClock(message.getLamportClock());
        conversation.addMessage(message);
    }

    // [MODIFIED]: Sử dụng lamportClock thay vì messageId
    private void onSendSuccessMessage(long lamportClock, String conversationId){
        Conversation conversation = Cache.getInstance().getConversation(conversationId);
        IO.println("ID cua conversation dag tim" + conversationId);
        List<Conversation> lst_con= Cache.getInstance().getConversationList();
        for (Conversation c : lst_con) {
            IO.println("---------------------------------");
            IO.println("ID: " + c.getId()); // Lấy ID của Conversation
            IO.println("Tên: " + c.getName()); // Lấy tên của Conversation

            if (c instanceof DirectConversation) {
                DirectConversation d = (DirectConversation) c;
                IO.println("Loại: Direct Chat");
                // Lấy ID của đối tác
                IO.println("Đối tác ID: " + d.getPartner().getId());
            } else if (c instanceof GroupConversation) {
                GroupConversation g = (GroupConversation) c;
                IO.println("Loại: Group Chat");
                // Lấy số lượng thành viên
                IO.println("Số thành viên: " + g.getParticipantList().size());
            } else {
                IO.println("Loại: Unknown");
            }

            // In trạng thái tin nhắn
            IO.println("Tin nhắn thành công: " + c.getSuccessMessage().size());
            IO.println("Tin nhắn đang chờ: " + c.getPendingMessage().size());
            IO.println("Last Lamport Clock: " + c.getLamportClock()); // Lamport Clock cao nhất
        }

        if(conversation == null) {
            Logger.warn("Bi null me roi");
            return;
        }

        conversation.getMessageList().stream()
                .filter(m->(m.getLamportClock() == lamportClock))
                .findFirst()
                .ifPresent(
                        message -> {
                            message.setStatus(Message.MessageStatus.SUCCESS);
                            Logger.info("Message Clock " + lamportClock + " in Conv " + conversationId + " confirmed SUCCESS.");
                        }
                );
    }

    // Giữ nguyên logic cũ
    private void onSendFailedMessage(String messageId, String conversationId){
        Conversation conversation = Cache.getInstance().getConversation(conversationId);
        if(conversation == null)
            return;

        conversation.getMessageList().stream()
                .filter(m->(m.getId().equals(messageId)))
                .findFirst()
                .ifPresent(
                        message -> {
                            message.setStatus(Message.MessageStatus.FAILED);
                            Logger.warn("Message ID " + messageId + " in Conv " + conversationId + " marked FAILED.");
                        }
                );
    }
}