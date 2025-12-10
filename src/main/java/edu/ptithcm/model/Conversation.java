package edu.ptithcm.model;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class Conversation {
    protected final String id;
    protected final String name;
    protected final ConcurrentSkipListSet<Message> messages;

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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Iterator<Message> getMessages(){
        return this.messages.iterator();
    }

}
