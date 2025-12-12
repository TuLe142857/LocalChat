package edu.ptithcm.bus.event;

import edu.ptithcm.model.Message;

public class MessageReceivedEvent {
    private final Message message;

    /**
     * <pre>
     *     Create MessageReceivedEvent
     *     This constructor will call message.setStatus(Message.MessageStatus.SUCCESS);
     * </pre>
     * @param message message received from other peer
     */
    public MessageReceivedEvent(Message message) {
        message.setStatus(Message.MessageStatus.SUCCESS);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
