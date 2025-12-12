package edu.ptithcm.model;

import edu.ptithcm.security.Signable;

import java.util.UUID;

public class Message implements Comparable<Message>, Signable {
    public static enum MessageStatus{
        PENDING,
        SUCCESS,
        FAILED
    }
    private final String id;  //UUID
    private final String conversationId;
    private final String content;
    private final String senderId;
    private final long timestamp;
    private final long lamportClock;
    private MessageStatus status;

    private String signature;

    /**
     * DO NOT USE THIS TO CREATE NEW MESSAGE
     * TO CONTROL LAMPORT CLOCK, USE method in class Conversation instead
     */
    public Message(String conversationId, String senderId, String content, long lamportClock){
        this.id = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.content = content;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.lamportClock = lamportClock;
        this.status = MessageStatus.PENDING; // Default status for outgoing messages
    }

    // NEW CONSTRUCTOR for incoming messages (to set initial status)
    public Message(String conversationId, String senderId, String content, long lamportClock, MessageStatus status){
        this.id = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.content = content;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.lamportClock = lamportClock;
        this.status = status;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getContent() {
        return content;
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getLamportClock() {
        return lamportClock;
    }

    @Override
    public int compareTo(Message m) {
        if(this.getLamportClock() < m.getLamportClock())
            return -1;
        if(this.getLamportClock() > m.getLamportClock())
            return 1;
        return this.getId().compareTo(m.getId());
    }

    @Override
    public String getSignableData() {
        return id + conversationId + content + senderId + timestamp + lamportClock;
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