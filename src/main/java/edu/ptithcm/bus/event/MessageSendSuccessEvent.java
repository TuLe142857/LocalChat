package edu.ptithcm.bus.event;

public class MessageSendSuccessEvent {
    private final String messageId;
    private final String conversationId;

    public MessageSendSuccessEvent(String messageId, String conversationId) {
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
