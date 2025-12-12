package edu.ptithcm.bus.event;

public class MessageSendFailedEvent {
    private final String messageId;
    private final String conversationId;

    public MessageSendFailedEvent(String messageId, String conversationId) {
        this.messageId = messageId;
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getConversationId() {
        return conversationId;
    }
}
