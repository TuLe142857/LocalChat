package edu.ptithcm.network;

import module java.base;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Credential;
import edu.ptithcm.model.Peer;
import edu.ptithcm.network.core.ConnectionPool;
import edu.ptithcm.network.packet.DiscoveryPayload;
import edu.ptithcm.network.packet.HandshakePayload;
import edu.ptithcm.network.service.DiscoveryService;
import edu.ptithcm.network.service.HandshakeService;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.service.AuthService;
import edu.ptithcm.util.JsonUtils;

public class NetworkService {

    //udp
    private static final int discoveryUnicastPort = 9999;
    private static final int discoveryMulticastPort = 9998;
    private static final InetAddress discoveryMulticastGroup;

    static {
        try {
            discoveryMulticastGroup = InetAddress.getByName("230.0.0.1");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private ScheduledExecutorService scheduledExecutorService;
    private HandshakeService handshakeService;
    private DiscoveryService discoveryService;
    private final InetAddress bindAddress;
    private final int tcpPort;

    public NetworkService(InetAddress bindAddress, int tcpPort){
        this.bindAddress = bindAddress;
        this.tcpPort = tcpPort;
    }

    public void start(){
        handshakeService = new HandshakeService(bindAddress, tcpPort, this::handleHandshakeClient);
        discoveryService = new DiscoveryService(
                discoveryUnicastPort,
                discoveryMulticastPort,
                discoveryMulticastGroup,
                bindAddress,
                this::handleDiscoveryUnicast,
                this::handleDiscoveryMulticast
        );

        scheduledExecutorService = Executors.newScheduledThreadPool(
                1,
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                    }
        );
        scheduledExecutorService.scheduleWithFixedDelay(this::sendDiscoverMulticast, 3, 5, TimeUnit.SECONDS);

        //start service
        handshakeService.start();
        discoveryService.start();
    }

    public void stop(){
        IO.println("Network service stop");
        scheduledExecutorService.shutdownNow();
        handshakeService.stop();
        discoveryService.stop();
    }

    private void handleHandshakeClient(Socket client){
        try{
            DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
            int length = dataInputStream.readInt();
            byte[] buf = new byte[length];
            dataInputStream.readFully(buf);
            NetworkPacket networkPacket = NetworkPacket.fromBytes(buf, 0, buf.length);

            if(networkPacket.getPacketType() != NetworkPacket.PacketType.HANDSHAKE){
                client.close();
                return;
            }

            HandshakePayload handshakePayload = networkPacket.getPayloadAs(HandshakePayload.class);
            Peer senderPeer = Cache.getInstance().getPeer(handshakePayload.getSenderId());
            boolean check = (senderPeer != null)
                    && (System.currentTimeMillis() - handshakePayload.getTimestamp() < 5000)
                    && (handshakePayload.verify(senderPeer.getPublicKey()));
            if (!check){
                client.close();
                return;
            }


            String encryptedSessionKeyStr = handshakePayload.getEncryptedSessionKey();
            String plainSessionKeyStr = CryptoUtils.decryptRSA(encryptedSessionKeyStr, Cache.getInstance().getCredential().getPrivateKey());
            if(plainSessionKeyStr == null){
                client.close();
                return;
            }
            SecretKey sessionKey = CryptoUtils.stringToSecretKey(plainSessionKeyStr);

            // logic xử lý concurrency khi 2 peer cùng yêu cầu handshake cùng lúc sử lý ở ConnectionPool.addConnection
            boolean addSuccess = ConnectionPool.getInstance().addConnection(senderPeer, client, sessionKey);
            if(!addSuccess){
                client.close();
                return;
            }


        } catch (Exception e) {

//            throw new RuntimeException(e);
        }

    }

    private void handleDiscoveryUnicast(DatagramPacket packet){
        IO.println("Get udp unicast discover from " + packet.getSocketAddress());
        NetworkPacket networkPacket = NetworkPacket.fromDatagramPacket(packet);
        if(networkPacket.getPacketType() != NetworkPacket.PacketType.DISCOVER)
            return;
        DiscoveryPayload discoveryPayload = networkPacket.getPayloadAs(DiscoveryPayload.class);
        boolean check = discoveryPayload.verify(discoveryPayload.getPeer().getPublicKey())
                && (System.currentTimeMillis() - discoveryPayload.getTimestamp() < 3000);
        if (!check)
            return;
        Cache.getInstance().addPeer(discoveryPayload.getPeer());
    }

    private void handleDiscoveryMulticast(DatagramPacket packet){
        IO.println("Get udp multicast discover from " + packet.getSocketAddress());
        NetworkPacket networkPacket = NetworkPacket.fromDatagramPacket(packet);
        if(networkPacket.getPacketType() != NetworkPacket.PacketType.DISCOVER)
            return;
        DiscoveryPayload discoveryPayload = networkPacket.getPayloadAs(DiscoveryPayload.class);
        boolean check = discoveryPayload.verify(discoveryPayload.getPeer().getPublicKey())
                && (System.currentTimeMillis() - discoveryPayload.getTimestamp() < 3000);
        if (!check){
            IO.println("Verify failed");
            IO.println(discoveryPayload.verify(discoveryPayload.getPeer().getPublicKey()));
            IO.println((System.currentTimeMillis() - discoveryPayload.getTimestamp() < 3000));
        }

        //fix self broadcast
        if(discoveryPayload.getPeer().getId().compareTo(Cache.getInstance().getCredential().getId()) != 0){
            IO.println("try add cache");
            Cache.getInstance().addPeer(discoveryPayload.getPeer());
        }else{
            IO.println("ignore");
            return;
        }


        // send reply discovery
        try{
            Peer myPeer = Cache.getInstance().getMyPeer();
            String payload = JsonUtils.toJson(myPeer);
            NetworkPacket replyDiscovery = new NetworkPacket(NetworkPacket.PacketType.DISCOVER, payload);
            this.discoveryService.sendUnicast(replyDiscovery.toBytes(), discoveryPayload.getPeer().getIp(), discoveryPayload.getPeer().getPort());
            IO.println("Send reply discover ok");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendDiscoverMulticast(){
        try{
            IO.println("Send discovery multicast");
            Peer myPeer = Cache.getInstance().getMyPeer();
            if(myPeer == null)
                return;
            DiscoveryPayload discoveryPayload = new DiscoveryPayload(myPeer);
            discoveryPayload.sign(Cache.getInstance().getCredential().getPrivateKey());
            String payload = JsonUtils.toJson(discoveryPayload);
            NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.DISCOVER, payload);
            this.discoveryService.sendMulticast(networkPacket.toBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    static void main() throws UnknownHostException {
        AuthService.login(
                new Credential(CryptoUtils.generateRSAKeyPair(), "Tú(window)"),
                InetAddress.getByName("192.168.65.1"),
                9999);
        NetworkService networkService = new NetworkService(InetAddress.getByName("192.168.65.1"), 9999);
        networkService.start();

        while(true){}
    }

}
