package edu.ptithcm.model;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Message.MessageStatus;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class Conversation {
    protected final String id;
    protected String name;
    // Sử dụng CopyOnWriteArrayList để đảm bảo an toàn thread
    protected final List<Message> messages;
    protected long lastLamportClock; // Clock cao nhất của tin nhắn trong Conversation

    public Conversation(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.messages = new CopyOnWriteArrayList<>();
        this.lastLamportClock = Cache.getInstance().getLamportClock();
    }

    public Conversation(String name, String id) {
        this.id = id;
        this.name = name;
        this.messages = new CopyOnWriteArrayList<>();
        this.lastLamportClock = Cache.getInstance().getLamportClock();
    }

    /**
     * Tạo tin nhắn gởi đi
     * @param content
     * @return
     */
    public synchronized Message createMessage(String content){
        // 1. Tăng Lamport Clock toàn cục và của Conversation
        long currentClock = Cache.getInstance().incrementLamportClock();
        this.lastLamportClock = currentClock;

        // 2. Tạo Message
        Message message = new Message(
                this.id,
                Cache.getInstance().getCredential().getId(),
                content,
                currentClock
        );
        message.setStatus(MessageStatus.PENDING);

        // 3. Thêm vào danh sách tin nhắn
        addMessage(message);

        Logger.debug("Created message with clock: " + currentClock);
        return message;
    }

    /**
     * Thêm tin nhắn đã có (dùng cho tin nhắn nhận được hoặc tin nhắn đã tồn tại)
     * @param message
     */
    public synchronized void addMessage(Message message){
        // Cập nhật Lamport Clock của Conversation khi có tin nhắn mới (chủ yếu cho mục đích sắp xếp)
        if (message.getLamportClock() > this.lastLamportClock) {
            this.lastLamportClock = message.getLamportClock();
        }

        this.messages.add(message);

        // Sắp xếp lại danh sách theo Lamport Clock và ID
        this.messages.sort(Comparator.comparing(Message::getLamportClock).thenComparing(Message::getId));
    }

    /**
     * Tìm kiếm tin nhắn dựa trên Lamport Clock (dùng cho việc nhận Message ACK)
     * @param clock Lamport Clock của tin nhắn cần tìm
     * @return Message hoặc null
     */
    public Message getMessageByClock(long clock) {
        for (Message message : messages) {
            if (message.getLamportClock() == clock) {
                return message;
            }
        }
        return null;
    }


    public long getLamportClock() {
        return lastLamportClock;
    }

    public void setLamportClock(long lamportClock) {
        this.lastLamportClock = lamportClock;
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