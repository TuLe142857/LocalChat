package edu.ptithcm.network.packet.payload;

import edu.ptithcm.model.Message;
import edu.ptithcm.security.Signable;

import java.util.List;

public class FetchMessageResponsePayload implements Signable {
    private final String senderId;
    private final String conversationId;
    private final List<Message> messages;

    private final long timestamp;
    private String signature;

    public FetchMessageResponsePayload(String senderId, String conversationId, List<Message> messages) {
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.messages = messages;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignableData() {
        return senderId + conversationId + timestamp;
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
