package edu.ptithcm.model;

public class DirectConversation extends Conversation{
    private final Peer partner;

    public DirectConversation(Peer partner){
        super(partner.getName(), partner.getId());
        this.partner = partner;
    }

    public Peer getPartner(){
        return partner;
    }


}
