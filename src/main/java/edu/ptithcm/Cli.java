package edu.ptithcm;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageSendingEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.*;
import edu.ptithcm.network.NetworkService;
import edu.ptithcm.security.CredentialManager;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.service.AuthService;
import edu.ptithcm.service.ChatService;
import edu.ptithcm.util.JsonUtils;
import edu.ptithcm.util.LogConfig;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

public class Cli {
    static Map<String, Consumer<String[]>> commandHandler = Map.of(
            "cache", Cli::showCache,
            "chat", Cli::chat,
            "create-group", Cli::create_group,
            "exit", Cli::exit,
            "help", Cli::help
    );
    static NetworkService networkService;

    static void main() throws Exception{
        LogConfig.config(false, true);

        Credential credential = CredentialManager.readStoredCredential();
        if(
                credential == null
                || (IO.readln("Found Stored Credential, login with this credential?(y/n) ").toLowerCase().equals("n"))
        ){
            String name = IO.readln("Your name: ");
            credential = new Credential(CryptoUtils.generateRSAKeyPair(), name);
        }else{
            IO.println("Name: " + credential.getName());
        }

        InetAddress ip = InetAddress.getByName(IO.readln("ip: "));
        int port = Integer.parseInt(IO.readln("Port: "));

        AuthService.login(credential, ip, port);
        ChatService.init();
        networkService = new NetworkService(ip, port);
        networkService.start();

        while(true){
            String command = IO.readln(">> ");
            String []args = command.split("\\s+");
            var handler = commandHandler.get(args[0]);
            if(handler != null){
                handler.accept(args);
            }
        }
    }

    static void showCache(String []args){
        IO.println("===============================");
        IO.println("       Data stored in cache");
        IO.println("===============================");
        IO.println("Self information:");
        IO.println("Id: " + Cache.getInstance().getCredential().getId());
        IO.println("PublicKey: " + CryptoUtils.publicKeyToString(Cache.getInstance().getCredential().getPublicKey()));
        IO.println("PrivateKey: "+CryptoUtils.privateKeyToString(Cache.getInstance().getCredential().getPrivateKey()));
        IO.println("Name: "+ Cache.getInstance().getCredential().getName());
        IO.println("IP: " + Cache.getInstance().getIp());;
        IO.println("Port: " +Cache.getInstance().getPort());
        IO.println();

        IO.println("Known Peer list: ");
        for (var entry : Cache.getInstance().getPeerEntrySet()){
            Peer peer = entry.getValue();
            IO.println("Id: " + peer.getId());
            IO.println("PublicKey: " + CryptoUtils.publicKeyToString(peer.getPublicKey()));
            IO.println("Name: " + peer.getName());
            IO.println("IP: " + peer.getIp());
            IO.println("Port: "+ peer.getPort());
            IO.println();
        }


        IO.println("Conversation list:");
        for (var c : Cache.getInstance().getConversationList()){
            IO.println("Id: " + c.getId());
            IO.println("Name: " + c.getName());
            IO.println("Type: " + ((c instanceof DirectConversation)?("DirectConversation"):("GroupConversation")));
            if(c instanceof GroupConversation){
                GroupConversation g = (GroupConversation)(c);
                IO.println("Number of participants: " + g.getParticipantList().size());
            }
            else{
                DirectConversation d = (DirectConversation) (c);
                IO.println("Partner Id: " + d.getPartner().getId());
            }
            IO.println("Number of success message: " + c.getSuccessMessage().size());
            IO.println("Number of pending message: " + c.getPendingMessage().size());
            IO.println("Number of send failed message: " + c.getFailedMessage().size());
            IO.println();
        }
    }

    static void chat(String []args){
        String peerId = args[1];
        IO.println("Chat with peer id: "+ args[1]);
        Peer partner = Cache.getInstance().getPeer(peerId);
        if(partner == null){
            IO.println("Peer null");
            return;
        }

        Conversation conversation = Cache.getInstance().getConversation(peerId);
        if(conversation == null){
            conversation = new DirectConversation(partner);
            Cache.getInstance().addConversation(conversation);
            IO.println("<Create new conversation>");
        }
        else{
            IO.println("<message in this conversation>");
            for(var m:conversation.getSuccessMessage()){
                if(m.getSenderId().equals(Cache.getInstance().getCredential().getId())){
                    IO.println("You: " +m.getContent());
                }
                else {
                    IO.println(partner.getName() + ": " + m.getContent());
                }


            }
            IO.println("<--------------------->");
        }

        String content = IO.readln(">> Message: ");
        Message message = conversation.createMessage(content);
        MessageBus.emit(new MessageSendingEvent(message));
    }

    static void create_group(String args[]){
        String name = args[1];
        ArrayList<Peer> invitedPeer = new ArrayList<>();
        for(int i = 2; i < args.length; i++){
            Peer p = Cache.getInstance().getPeer(args[i]);
            if(p == null){
                IO.println("Null peer for id " + args[i]);
                IO.println("Cancelled create group");
            }
            invitedPeer.add(p);
        }
//        ChatService.createGroupConversation(name, invitedPeer);
    }

    static void help(String []args){
        IO.println("Available command:");
        for(var entry:commandHandler.entrySet()){
            IO.println(entry.getKey());
        }
        IO.println();
    }
    static void exit(String []args){
        networkService.stop();
        System.exit(0);
    }
}