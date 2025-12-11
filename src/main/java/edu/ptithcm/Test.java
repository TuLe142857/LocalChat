package edu.ptithcm;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageSendingEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.*;
import edu.ptithcm.network.NetworkService;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.service.AuthService;
import edu.ptithcm.service.ChatService;
import edu.ptithcm.util.JsonUtils;
import edu.ptithcm.util.LogConfig;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Test {
    static CompletableFuture<Integer> calcSum(int a, int b){
        CompletableFuture<Integer> res = new CompletableFuture<>();
        Thread t = new Thread(()->{
            try {
                Thread.sleep(5000); // 5 second
                res.complete(a +  b);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();

        return res;
    }
    static void main(){
        IO.println("Test CompleableFuture<T> ");
        CompletableFuture<Integer> future = calcSum(10, 20);
        future.thenAccept(result->IO.println("Kê quá tính toán:"+result));
        IO.println("Thấy chưa, việc tính toán dù delay 5s luồng chính vẫn không bị chặn");
    }

    public static class Cli {
        static Map<String, Consumer<String[]>> commandHandler = Map.of(
                "cache", Cli::showCache,
                "chat", Cli::chat,
                "exit", Cli::exit
        );
        static NetworkService networkService;
        static void main() throws Exception{
            LogConfig.config(false, true);
            String name = IO.readln("Your name: ");
            InetAddress ip = InetAddress.getByName(IO.readln("ip: "));
            int port = Integer.parseInt(IO.readln("Port: "));

            Credential credential = new Credential(CryptoUtils.generateRSAKeyPair(), name);
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
            IO.println("Peer list: ");
            for (var entry : Cache.getInstance().getPeerEntrySet()){
                IO.println(JsonUtils.toJson(entry.getValue()));
            }


            IO.println("Conversation list:");
            for (var c : Cache.getInstance().getConversationList()){
                IO.println(JsonUtils.toJson(c));
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
            }
            else{
                for(var m:conversation.getSuccessMessage()){
                    if(m.getSenderId().equals(Cache.getInstance().getCredential().getId())){
                        IO.println("You: " +m.getContent());
                    }
                    else {
                        IO.println(partner.getName() + ": " + m.getContent());
                    }

                }
            }

            String content = IO.readln("Message: ");
            Message message = conversation.createMessage(content);
            MessageBus.emit(new MessageSendingEvent(message));
        }

        static void exit(String []args){
            networkService.stop();
            System.exit(0);
        }
    }
}
