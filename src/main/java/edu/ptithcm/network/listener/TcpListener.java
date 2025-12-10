package edu.ptithcm.network.listener;

import module java.base;
public class TcpListener {

    @FunctionalInterface
    public static interface TcpHandler{
        void handleClient(Socket client);
    }

    private volatile boolean running;
    private final InetAddress bindAddress;
    private final int bindPort;
    private ServerSocket serverSocket;
    private final TcpHandler tcpHandler;

    public TcpListener(InetAddress bindAddress, int bindPort, TcpHandler tcpHandler){
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
        this.tcpHandler = tcpHandler;
    }

    public void start() {
        running = true;
        IO.println("TCP listener start on "+bindAddress +":"+bindPort);

        // try start server socket
        try{
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(bindAddress, bindPort));
        }catch (IOException e){
            stop();
        }

        // loop while(running): handle client
        try{
            while (running){
                Socket client = serverSocket.accept();
                this.tcpHandler.handleClient(client);
            }
        }catch (IOException e){
            stop();
        }
    }


    public void stop(){
        IO.println("TCP listener stop");
        try{
            running = false;
            if (this.serverSocket != null && !this.serverSocket.isClosed())
                this.serverSocket.close();
        }
        catch (Exception e){}
    }
}
