package edu.ptithcm.bus.event;

import edu.ptithcm.model.Message;

public class MessageReceivedEvent {
    private final Message message;

    public MessageReceivedEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
