package edu.ptithcm.network.service;

import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.util.JsonUtils;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscoveryService {
    @FunctionalInterface
    public static interface PacketHandler {
        void handlePacket(DatagramPacket packet);
    }

    private final InetAddress bindIp;
    private final int unicastPort;
    private final int multicastPort;
    private final InetAddress multicastGroup;

    private DatagramSocket unicastSocket;
    private MulticastSocket multicastSocket;
    private volatile boolean running;

    private ExecutorService executor;

    private final PacketHandler unicastPacketHandler;
    private final PacketHandler multicastPacketHandler;

    public DiscoveryService(int unicastPort, int multicastPort, InetAddress multicastGroup, InetAddress bindIp, PacketHandler unicastPacketHandler, PacketHandler multicastPacketHandler) {
        this.unicastPort = unicastPort;
        this.multicastPort = multicastPort;
        this.multicastGroup = multicastGroup;
        this.bindIp = bindIp; // IP LAN mà người dùng chọn ở LoginView
        this.unicastPacketHandler = unicastPacketHandler;
        this.multicastPacketHandler = multicastPacketHandler;
        this.running = false;
    }

    public void start() {
        if (running) return;
        running = true;
        executor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            // 1. Unicast Socket
            unicastSocket = new DatagramSocket(new InetSocketAddress(bindIp, unicastPort));
            IO.println("Discovery Unicast listening on " + bindIp + ":" + unicastPort);

            // 2. Multicast Socket
            multicastSocket = new MulticastSocket(multicastPort);
            NetworkInterface ni = NetworkInterface.getByInetAddress(bindIp);
            if (ni != null) {

                InetSocketAddress groupAddress = new InetSocketAddress(multicastGroup, multicastPort);
                multicastSocket.joinGroup(groupAddress, ni);
                multicastSocket.setNetworkInterface(ni);
                IO.println("Discovery Multicast joined group " + multicastGroup + ":" + multicastPort + " on " + ni.getName());
            } else {
                IO.println("Error: Could not find network interface for " + bindIp);
            }

            // 3. Start listening
            executor.execute(this::listenUnicast);
            executor.execute(this::listenMulticast);

        } catch (IOException e) {
            e.printStackTrace();
            stop();
        }
    }

    public void stop() {
        running = false;

        // Đóng Unicast
        if (unicastSocket != null && !unicastSocket.isClosed()) {
            unicastSocket.close();
        }

        // Đóng Multicast
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                // Leave group lịch sự trước khi đóng
                NetworkInterface ni = NetworkInterface.getByInetAddress(bindIp);
                if (ni != null) {
                    multicastSocket.leaveGroup(new InetSocketAddress(multicastGroup, multicastPort), ni);
                }
                multicastSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (executor != null) {
            executor.close();
        }
        IO.println("Discovery Service stopped");
    }

    public void listenUnicast() {
        byte[] buffer = new byte[4096];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                unicastSocket.receive(packet);
                if (unicastPacketHandler != null) {
                    unicastPacketHandler.handlePacket(packet);
                }
            } catch (IOException e) {
                // Socket closed, exit loop
                if (running) e.printStackTrace();
            }
        }
    }

    public void listenMulticast() {
        byte[] buffer = new byte[4096];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                if (multicastPacketHandler != null) {
                    multicastPacketHandler.handlePacket(packet);
                }
            } catch (IOException e) {
                // Socket closed, exit loop
                if (running) e.printStackTrace();
            }
        }
    }

    public synchronized void sendUnicast(byte[] buf, InetAddress targetIp, int targetPort) throws IOException {
        DatagramPacket packet = new DatagramPacket(buf, buf.length, targetIp, targetPort);
        unicastSocket.send(packet);
    }

    public synchronized void sendMulticast(byte[] buff) throws IOException {
        DatagramPacket packet = new DatagramPacket(buff, buff.length, multicastGroup, multicastPort);
        multicastSocket.send(packet);
    }

//    static void main() throws IOException, InterruptedException {
//        DiscoveryService discoveryService = new DiscoveryService(
//                9999,
//                9998,
//                InetAddress.getByName("230.0.0.1"),
//                InetAddress.getByName("192.168.65.1"),
//                (packet -> {
//                    IO.println("Get packet unicast from" + packet.getSocketAddress());
//                    IO.println(JsonUtils.toJson(NetworkPacket.fromDatagramPacket(packet)));
//                }),
//                (packet -> {
//                    IO.println("Get packet multicast from" + packet.getSocketAddress());
//                    IO.println(JsonUtils.toJson(NetworkPacket.fromDatagramPacket(packet)));
//                })
//        );
//        discoveryService.start();
//
//        while(true){
//            NetworkPacket np = new NetworkPacket(
//                    NetworkPacket.PacketType.DISCOVER,
//                    "sender id",
//                    "pay load", "sig"
//            );
//            discoveryService.sendMulticast(np.toBytes());
//            discoveryService.sendUnicast(np.toBytes(), InetAddress.getByName("192.168.65.1"), 9999);
//            Thread.sleep(3000);
//        }
//    }
}