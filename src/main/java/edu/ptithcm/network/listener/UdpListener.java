package edu.ptithcm.network.listener;

import module java.base;


public class UdpListener {

    @FunctionalInterface
    public static interface UdpHandler{
        void handleUdpPackage(DatagramPacket packet);
    }

    private volatile boolean running;
    private final InetAddress bindAddress;
    private final int bindPort;
    private DatagramSocket datagramSocket;
    private final UdpHandler udpHandler;

    public UdpListener(InetAddress bindAddress, int bindPort, UdpHandler udpHandler) {
        this.running = false;
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
        this.udpHandler = udpHandler;
    }

    public void start(){
        running = true;
        IO.println("UDP listener start on " + bindAddress + ":" + bindPort);
        DatagramSocket socket;

        try{
            datagramSocket = new DatagramSocket(new InetSocketAddress(bindAddress, bindPort));
        }catch (IOException e){
            stop();
        }

        try{
            byte[] buffer = new byte[1024];
            while (running){
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                this.udpHandler.handleUdpPackage(packet);
            }
        } catch (IOException e) {
            stop();
        }
    }
    public void run() throws SocketException {
        running = true;
        IO.println("Start UDP listener");
        try(DatagramSocket socket = new DatagramSocket(new InetSocketAddress(bindAddress, bindPort))){
            datagramSocket = socket;
            byte[] buffer = new byte[1024];
            while (running){
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
            }
        } catch (IOException e) {
//            throw new RuntimeException(e);
        }
        IO.println("UPD listener stop");
    }

    public void stop(){
        IO.println("UDP listener stop1");
        running = false;
        if(datagramSocket != null && !datagramSocket.isClosed()){
            datagramSocket.close();
        }
    }
}