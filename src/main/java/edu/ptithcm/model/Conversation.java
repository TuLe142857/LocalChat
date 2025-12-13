package edu.ptithcm.model;

import edu.ptithcm.cache.Cache;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public abstract class Conversation {
    protected final String id;
    protected String name;
    protected final ConcurrentSkipListSet<Message> messages;
    protected long lamportClock;

    public Conversation(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.messages = new ConcurrentSkipListSet<>();
    }

    public Conversation(String name, String id) {
        this.id = id;
        this.name = name;
        this.messages = new ConcurrentSkipListSet<>();
    }

    /**
     * Tạo tin nhắn gởi đi(đã xử lý lamport clock + chữ ký số)
     * @param content
     * @return
     */
    public synchronized Message createMessage(String content){
        this.lamportClock +=1;
        Message message = new Message(
                this.id,
                Cache.getInstance().getCredential().getId(),
                content,
                this.lamportClock
        );
        message.setStatus(Message.MessageStatus.PENDING);
        message.sign(Cache.getInstance().getCredential().getPrivateKey());
        this.messages.add(message);
        return message;
    }

    /**
     * Tin nhắn nhận được
     * @param message
     */
    public synchronized void onReceiveMessage(Message message){
        if(this.lamportClock < message.getLamportClock())
            this.lamportClock = message.getLamportClock();
        this.messages.add(message);
    }

    public long getLamportClock() {
        return lamportClock;
    }

    public void setLamportClock(long lamportClock) {
        this.lamportClock = lamportClock;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Message> getMessageList(){
        return new ArrayList<>(this.messages);
    }

    public List<Message> getSuccessMessage(){
        return this.messages.stream()
                .filter(m -> m.getStatus() == Message.MessageStatus.SUCCESS)
                .collect(Collectors.toList());
    }

    public List<Message> getPendingMessage(){
        return this.messages.stream()
                .filter(m -> m.getStatus() == Message.MessageStatus.PENDING)
                .collect(Collectors.toList());
    }

    public List<Message> getFailedMessage(){
        return this.messages.stream()
                .filter(m -> m.getStatus() == Message.MessageStatus.FAILED)
                .collect(Collectors.toList());
    }
}
