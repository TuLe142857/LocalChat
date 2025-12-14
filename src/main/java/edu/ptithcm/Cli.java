package edu.ptithcm;

import edu.ptithcm.bus.MessageBus;
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
            "invite-group", Cli::invite_group,
            "leave-group", Cli::leave_group,
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
        for (var peer : Cache.getInstance().getPeerList()){
            IO.println("Id: " + peer.getId());
            IO.println("PublicKey: " + CryptoUtils.publicKeyToString(peer.getPublicKey()));
            IO.println("Name: " + peer.getName());
            IO.println("IP: " + peer.getIp());
            IO.println("Port: "+ peer.getPort());
            IO.println();
        }


        IO.println("Conversation list:");
        for (var c : Cache.getInstance().getConversationList()){
            printConversationDetail(c, false);
        }
    }

    static void chat(String []args){
        String conversation_id =  IO.readln("Conversation id: ");
        boolean isDirectConversation = (Cache.getInstance().getPeer(conversation_id) != null);

        Conversation conversation = Cache.getInstance().getConversation(conversation_id);


        if(conversation == null && isDirectConversation){
            conversation = new DirectConversation(Cache.getInstance().getPeer(conversation_id));
            Cache.getInstance().addConversation(conversation);
            IO.println("\tCreate direct conversation to peer "+ conversation_id);
        }
        if(conversation == null){
            IO.println("No conversation match id "+ conversation_id);
            return;
        }
        while (true){
            printConversationDetail(conversation, true);
            String content = IO.readln("\nType message to send(or enter to exit): \n");
            if(content.isEmpty())
                break;
            Message message = conversation.createMessage(content);
            ChatService.sendMessage(message);
        }
    }

    static void create_group(String []args){
        String gName = IO.readln("Group name: ");
        ArrayList<String> invitedPeerId = new ArrayList<>();
        while(true){
            String pid =IO.readln("Add peer to group(type id or enter to skip):");
            if(pid.isEmpty()){
                break;
            }else{
                invitedPeerId.add(pid);
            }
        }
        GroupConversation groupConversation = ChatService.createGroupConversation(gName, invitedPeerId);
        IO.println("Created new group with id = " + groupConversation.getId());
        IO.println("The selected peer was pending invited, group member will update when they're accept your invitation");
    }

    static void invite_group(String []args){
        String gId = IO.readln("Group Id");
        Conversation conversation = Cache.getInstance().getConversation(gId);
        if(!(conversation instanceof GroupConversation)){
            IO.println("Invalid Group ID");
            return;
        }
        ArrayList<String> invitedPeerId = new ArrayList<>();
        while(true){
            String pid =IO.readln("Add peer to group(type id or enter to skip):");
            if(pid.isEmpty()){
                break;
            }else{
                invitedPeerId.add(pid);
            }
        }
        for(String pId : invitedPeerId){
            ChatService.invitePeerToGroup(gId, pId);
        }
    }

    static void leave_group(String []args){
        String group_id = IO.readln("Group id: ");
        Conversation conversation = Cache.getInstance().getConversation(group_id);
        if(!(conversation instanceof GroupConversation)){
            return;
        }
        ChatService.leaveGroup(group_id);
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

    static void printConversationDetail(Conversation c, boolean showMessageDetail){
        IO.println("Id: " + c.getId());
        IO.println("Name: " + c.getName());
        IO.println("Type: " + ((c instanceof DirectConversation)?("DirectConversation"):("GroupConversation")));

        if(c instanceof GroupConversation){
            GroupConversation g = (GroupConversation)(c);
            IO.println("Number of participants: " + g.getParticipantList().size());

        }
        else {
            DirectConversation d = (DirectConversation) (c);
            IO.println("Partner Id: " + d.getPartner().getId());
        }
        IO.println("Number of success message: " + c.getSuccessMessage().size());
        IO.println("Number of pending message: " + c.getPendingMessage().size());
        IO.println("Number of send failed message: " + c.getFailedMessage().size());
        if(showMessageDetail){
            IO.println("--Message in this conversation--");
            for(var m:c.getSuccessMessage()){
                if(m.getSenderId().equals(Cache.getInstance().getCredential().getId())){
                    IO.println("You: " +m.getContent());
                }
                else {
                    IO.println(Cache.getInstance().getPeer(m.getSenderId()).getName() + ": " + m.getContent());
                }
            }
            IO.println("--------------------------------");
        }


    }
}