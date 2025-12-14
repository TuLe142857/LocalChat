package edu.ptithcm.network.service;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Peer;
import edu.ptithcm.network.packet.payload.DiscoveryPayload;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.service.SyncService;
import edu.ptithcm.util.JsonUtils;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscoveryService {
    private final InetAddress bindIp;
    private final int unicastPort;
    private final int multicastPort;
    private final InetAddress multicastGroup;

    private DatagramSocket unicastSocket;
    private MulticastSocket multicastSocket;
    private volatile boolean running;

    private ExecutorService virtualExecutorService;
    private ScheduledExecutorService scheduledExecutorService;

    public DiscoveryService(
            int unicastPort,
            int multicastPort,
            InetAddress multicastGroup,
            InetAddress bindIp) {
        this.unicastPort = unicastPort;
        this.multicastPort = multicastPort;
        this.multicastGroup = multicastGroup;
        this.bindIp = bindIp;
        this.running = false;
    }

    public void start() {
        if (running) return;
        running = true;
        virtualExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        scheduledExecutorService = Executors.newScheduledThreadPool(
                1,
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                }
                );
        scheduledExecutorService.scheduleWithFixedDelay(this::sendDiscoverMulticast, 0, 5, TimeUnit.SECONDS);

        try {
            // 1. Unicast Socket
            unicastSocket = new DatagramSocket(new InetSocketAddress(bindIp, unicastPort));
            Logger.info("Discovery Unicast listening on " + bindIp + ":" + unicastPort);

            // 2. Multicast Socket
            multicastSocket = new MulticastSocket(multicastPort);
            NetworkInterface ni = NetworkInterface.getByInetAddress(bindIp);
            if (ni != null) {

                InetSocketAddress groupAddress = new InetSocketAddress(multicastGroup, multicastPort);
                multicastSocket.joinGroup(groupAddress, ni);
                multicastSocket.setNetworkInterface(ni);
                Logger.info("Discovery Multicast joined group " + multicastGroup + ":" + multicastPort + " on " + ni.getName());
            } else {
                Logger.info("Error: Could not find network interface for " + bindIp);
            }

            // 3. Start listening
            virtualExecutorService.execute(this::listenUnicast);
            virtualExecutorService.execute(this::listenMulticast);

        } catch (IOException e) {
            Logger.error(e);
            stop();
        }
    }

    public void stop() {
        running = false;

        // stop send discover multicast
        this.scheduledExecutorService.shutdownNow();

        // stop listening
        this.virtualExecutorService.shutdownNow();

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
                Logger.error(e);
            }
        }
        Logger.info("Discovery Service stopped.");
    }

    public void listenUnicast() {
        byte[] buffer = new byte[4096];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                unicastSocket.receive(packet);
                this.handleDiscoveryPacket(packet, false);
            } catch (Exception e) {
                if (running)
                    Logger.error(e);
            }
        }
    }

    public void listenMulticast() {
        byte[] buffer = new byte[4096];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                this.handleDiscoveryPacket(packet, true);
            } catch (Exception e) {
                if (running)
                    Logger.error(e);
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

    private void sendDiscoverMulticast(){
        try{
            Peer myPeer = Cache.getInstance().getMyPeer();
            if(myPeer == null)
                return;
            DiscoveryPayload discoveryPayload = new DiscoveryPayload(myPeer);
            discoveryPayload.sign(Cache.getInstance().getCredential().getPrivateKey());
            String payload = JsonUtils.toJson(discoveryPayload);
            NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.DISCOVER, payload);
            this.sendMulticast(networkPacket.toBytes());
        }catch (Exception e){
            Logger.error(e);
        }
    }

    private void handleDiscoveryPacket(DatagramPacket packet, boolean isMulticast){
        NetworkPacket networkPacket = NetworkPacket.fromDatagramPacket(packet);
        if(networkPacket.getPacketType() != NetworkPacket.PacketType.DISCOVER){
            Logger.warn("Get NetworkPacket from " + packet.getSocketAddress()  + " but is not type DISCOVER ignore this packet");
            return;
        }

        DiscoveryPayload discoveryPayload = networkPacket.getPayloadAs(DiscoveryPayload.class);

        // check self-broadcast
        if(discoveryPayload.getPeer().getId().equals(Cache.getInstance().getCredential().getId())){
            return;
        }

        //verify
        boolean checkSignature = discoveryPayload.verify(discoveryPayload.getPeer().getPublicKey());
        boolean checkTimestamp = (System.currentTimeMillis() - discoveryPayload.getTimestamp() < 10000);
        if (!(checkSignature && checkTimestamp)){
            Logger.warn(
                String.format(
                    "Discovery packet(from %s) verify failed: %s",
                    (isMulticast?"multicast":"unicast"),
                    (!checkSignature)?"Signature verify failed":"timestamp too old (< 10 seconds from now)"
                )
            );
            return;
        }

        boolean peerExisting = Cache.getInstance().getPeer(discoveryPayload.getPeer().getId()) != null;
        if(peerExisting){
            Peer peer = Cache.getInstance().getPeer(discoveryPayload.getPeer().getId());
            // check info change
            if(!peer.getIp().equals(discoveryPayload.getPeer().getIp()))
                peer.setIp(discoveryPayload.getPeer().getIp());
            if(peer.getPort() != discoveryPayload.getPeer().getPort())
                peer.setPort(discoveryPayload.getPeer().getPort());
            if(!peer.getName().equals(discoveryPayload.getPeer().getName()))
                peer.setName(discoveryPayload.getPeer().getName());
        }else{
            Peer newPeer = discoveryPayload.getPeer();
            Cache.getInstance().addPeer(newPeer);
            Logger.debug(String.format(
                    "Discovery Service(%s) found new Peer:\n\tid: %s\n\tname: %s\n\tip: %s\n\tport: %d\n\t",
                    (isMulticast?"multicast":"unicast"),
                    newPeer.getId(),
                    newPeer.getName(),
                    newPeer.getIp().toString(),
                    newPeer.getPort()
            ));
            SyncService.requestSyncMetadata(newPeer);
        }

        // received from multicast -> send reply
        if(isMulticast){
            try{
                Peer myPeer = Cache.getInstance().getMyPeer();
                if(myPeer == null)
                    return;

                DiscoveryPayload discoveryPayloadReply = new DiscoveryPayload(myPeer);
                discoveryPayloadReply.sign(Cache.getInstance().getCredential().getPrivateKey());
                String payload = JsonUtils.toJson(discoveryPayloadReply);
                NetworkPacket replyDiscovery = new NetworkPacket(NetworkPacket.PacketType.DISCOVER, payload);
                this.sendUnicast(replyDiscovery.toBytes(), discoveryPayload.getPeer().getIp(), this.unicastPort);
            }catch (IOException e){
                Logger.error(e);
            }
        }
    }
}