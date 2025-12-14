package edu.ptithcm.bus.event;

import edu.ptithcm.model.Message;

public class MessageSendingEvent {
    private final Message message;

    public MessageSendingEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
