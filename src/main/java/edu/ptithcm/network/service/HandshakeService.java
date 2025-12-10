package edu.ptithcm.network.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandshakeService {
    @FunctionalInterface
    public static interface ClientHandler{
        void handleClient(Socket client);
    }

    private volatile boolean running;
    private final InetAddress bindAddress;
    private final int bindPort;
    private ServerSocket serverSocket;
    private final ClientHandler clientHandler;
    private ExecutorService executor;


    public HandshakeService(InetAddress bindAddress, int bindPort, ClientHandler clientHandler){
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
        this.clientHandler = clientHandler;
    }

    public void start() {
        if(running)
            return;
        running = true;
        executor = Executors.newVirtualThreadPerTaskExecutor();
        IO.println("TCP listener start on "+bindAddress +":"+bindPort);

        try{
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(bindAddress, bindPort));
        }catch (IOException e){
            stop();
        }

        executor.execute(this::listenTcp);
    }

    public void listenTcp(){
        try{
            while (running){
                Socket client = serverSocket.accept();
                this.clientHandler.handleClient(client);
            }
        }catch (IOException e){
            if (running)
                e.printStackTrace();
        }
    }


    public void stop(){
        IO.println("TCP listener stop");
        running = false;
        try{
            if(serverSocket != null)
                serverSocket.close();
        }catch (Exception e){}
        if(executor != null)
            executor.close();
    }
}
