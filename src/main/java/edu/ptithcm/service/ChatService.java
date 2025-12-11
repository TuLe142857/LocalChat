package edu.ptithcm.service;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendFailedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
import edu.ptithcm.bus.event.MessageSendingEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.*;
import edu.ptithcm.network.core.ConnectionPool;
import edu.ptithcm.network.core.PeerConnection;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.util.JsonUtils;

import java.util.concurrent.CompletableFuture;

public class ChatService {
    public static void init(){
        MessageBus.subscribe(
                MessageSendingEvent.class,
                messageSendingEvent -> {
                    ChatService.sendMessage(messageSendingEvent.getMessage());
                }
        );

        MessageBus.subscribe(
                MessageReceivedEvent.class,
                messageReceivedEvent -> {
                    ChatService.onReceiveMessage(messageReceivedEvent.getMessage());
                }
        );

        MessageBus.subscribe(
                MessageSendSuccessEvent.class,
                messageSendSuccessEvent -> {
                    ChatService.onSendSuccessMessage(messageSendSuccessEvent.getMessageId(), messageSendSuccessEvent.getConversationId());
                }
        );

        MessageBus.subscribe(
                MessageSendFailedEvent.class,
                messageSendFailedEvent -> {
                    ChatService.onSendFailedMessage(messageSendFailedEvent.getMessageId(), messageSendFailedEvent.getConversationId());
                }
        );

    }

    private static void sendMessage(Message message){
        Conversation conversation = Cache.getInstance().getConversation(message.getConversationId());
        if(conversation == null)
            return;
        if (conversation instanceof DirectConversation){
            DirectConversation dConversation = (DirectConversation)(conversation);
            Peer targetPeer = dConversation.getPartner();
            ConnectionPool.getInstance().getOrConnect(targetPeer)
                    .thenAccept(peerConnection -> {
                        NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.MESSAGE, JsonUtils.toJson(message));
                        try{
                            peerConnection.sendNetworkPacket(networkPacket);
                        }catch (Exception e){
                            MessageBus.emit(new MessageSendFailedEvent(message.getId(), message.getConversationId()));
                        }
                    })
                    .exceptionally(
                            t ->{
                                MessageBus.emit(new MessageSendFailedEvent(message.getId(), message.getConversationId()));
                                return  null;
                            }
                    );
        }else if(conversation instanceof GroupConversation){
            GroupConversation gConversation = (GroupConversation)(conversation);
            NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.MESSAGE, JsonUtils.toJson(message));
            for(Peer targetPeer: gConversation.getParticipantList()){

                // bỏ qua bản thân
                if (targetPeer.getId().equals(Cache.getInstance().getCredential().getId())) continue;

                ConnectionPool.getInstance().getOrConnect(targetPeer)
                        .thenAccept(
                                peerConnection -> {
                                    try{
                                        peerConnection.sendNetworkPacket(networkPacket);
//                                        allSendFailed = false;
                                    }catch (Exception e){
                                        MessageBus.emit(new MessageSendFailedEvent(message.getId(), message.getConversationId()));
                                    }
                                }
                        )
                        .exceptionally(
                                t->{
                                    MessageBus.emit(new MessageSendFailedEvent(message.getId(), message.getConversationId()));
                                    return  null;
                                }
                        );
            }
        }
    }

    private static void onReceiveMessage(Message message){
        String conversationId = (message.getConversationId().equals(Cache.getInstance().getCredential().getId()))
                ? (message.getSenderId())
                : (message.getConversationId());
        Conversation conversation = Cache.getInstance().getConversation(conversationId);
        if(conversation == null)
            return;
        conversation.onReceiveMessage(message);
    }

    private static void onSendSuccessMessage(String messageId, String conversationId){
        Conversation conversation = Cache.getInstance().getConversation(conversationId);
        if(conversation == null)
            return;
        conversation.getPendingMessage()
                .stream()
                .filter(m->(m.getId().equals(messageId)))
                .findFirst()
                .ifPresent(
                        message -> {message.setStatus(Message.MessageStatus.SUCCESS);}
                );
    }

    private static void onSendFailedMessage(String messageId, String conversationId){
        Conversation conversation = Cache.getInstance().getConversation(conversationId);
        if(conversation == null)
            return;
        conversation.getFailedMessage()
                .stream()
                .filter(m->(m.getId().equals(messageId)))
                .findFirst()
                .ifPresent(
                        message -> {message.setStatus(Message.MessageStatus.FAILED);}
                );
    }

}
