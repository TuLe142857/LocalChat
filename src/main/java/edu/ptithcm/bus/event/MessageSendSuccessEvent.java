package edu.ptithcm.bus.event;

public class MessageSendSuccessEvent {
    // SỬA: Thay thế messageId bằng lamportClock để đồng bộ với network ACK
    private final long lamportClock;
    private final String conversationId;

    public MessageSendSuccessEvent(long lamportClock, String conversationId) {
        this.lamportClock = lamportClock;
        this.conversationId = conversationId;
    }

    public long getLamportClock() {
        return lamportClock;
    }

    public String getConversationId() {
        return conversationId;
    }
}