package edu.ptithcm.network.packet;

public class MessagePayload {
    private String conversationId;
    private String senderId;
    private String content;
    private long lamportClock;

    // GSON required no-args constructor
    public MessagePayload() {}

    public MessagePayload(String conversationId, String senderId, String content, long lamportClock) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.lamportClock = lamportClock;
    }

    // Getters
    public String getConversationId() {
        return conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public long getLamportClock() {
        return lamportClock;
    }
}