package edu.ptithcm.network.packet;

import edu.ptithcm.security.Signable;

/**
 * Thông báo bên kia biết đã nhận được tin nhắn
 * Gói tin thông quan trọng, không cần xác thực bằng chữ ký số
 */
public class MessageAckPayload{
    private final String messageId;
    private final String conversationId;
    private final long timestamp;

    public MessageAckPayload(String messageId, String conversationId) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessageId() {
        return messageId;
    }

    // Getters
    public String getConversationId() {
        return conversationId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
