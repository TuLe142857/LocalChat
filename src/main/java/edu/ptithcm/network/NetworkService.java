package edu.ptithcm.network;

import module java.base;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Credential;
import edu.ptithcm.model.Peer;
import edu.ptithcm.network.core.ConnectionPool;
import edu.ptithcm.network.core.PeerConnection;
import edu.ptithcm.network.packet.DiscoveryPayload;
import edu.ptithcm.network.packet.HandshakeAckPayload;
import edu.ptithcm.network.packet.HandshakePayload;
import edu.ptithcm.network.service.DiscoveryService;
import edu.ptithcm.network.service.HandshakeService;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.service.AuthService;
import edu.ptithcm.service.SyncService;
import edu.ptithcm.util.JsonUtils;
import edu.ptithcm.util.LogConfig;
import org.tinylog.Logger;

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
        Logger.info("Network service stop");
        scheduledExecutorService.shutdownNow();
        handshakeService.stop();
        discoveryService.stop();
    }

    /**
     * Serversocket nhận đc yêu cầu handshake từ máy khác
     * Đọc gói tin handshake
     * Kiểm tra chữ ký số, thử thêm vào pool
     * Logic xử lý concurrency handshake được xử lý bên ConnectionPool, gọi hàm addIncomingConnection là được
     * @param client
     */
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
                    && senderPeer.getId().equals(CryptoUtils.hashSHA256(CryptoUtils.publicKeyToString(senderPeer.getPublicKey())))
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

            // logic xử lý concurrency khi 2 peer cùng yêu cầu handshake cùng lúc sử lý ở ConnectionPool.addIncomingConnection
            boolean addSuccess = ConnectionPool.getInstance().addIncomingConnection(senderPeer, client, sessionKey);
            HandshakeAckPayload handshakeAckPayload = new HandshakeAckPayload(Cache.getInstance().getCredential().getId(), addSuccess);
            handshakeAckPayload.sign(Cache.getInstance().getCredential().getPrivateKey());

            String encryptedPayload = CryptoUtils.encryptAES(JsonUtils.toJson(handshakeAckPayload), sessionKey);
            NetworkPacket ackPacket = new NetworkPacket(NetworkPacket.PacketType.HANDSHAKE_ACK, encryptedPayload);

            DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
            byte[] ackBuf =ackPacket.toBytes();
            dataOutputStream.writeInt(ackBuf.length);
            dataOutputStream.write(ackBuf);
            dataOutputStream.flush();

            if(!addSuccess){
                client.close();
            }


        } catch (Exception e) {

            // :))
            try{client.close();}catch (Exception ee){}
//            throw new RuntimeException(e);
        }

    }

    /**
     * <pre>
     *     Thực hiện gởi yêu cầu handshake
     *     Tạo session key, gởi gói handshake
     *     Nhận và check handshake ack, nếu verify ok và ack ==  accept thì trả về PeerConnection
     *     Việc thêm vào connection pool phải do luồng gọi hàm này tự xủ lý
     *
     * </pre>
     * @param targetPeer
     * @return
     * @throws Exception Nếu không thành công, tự động đóng socket
     */
    public static PeerConnection performOutgoingHandshake(Peer targetPeer) throws Exception {
        Logger.info("Start handshake with " + targetPeer.getIp());

        // 1. Mở Socket (Blocking I/O nhưng chạy trên Virtual Thread nên OK)
        Socket socket = new Socket(targetPeer.getIp(), targetPeer.getPort());
        socket.setSoTimeout(5000); // Timeout 5s cho handshake

        try {
            // 2. Tạo Session Key (AES) cho phiên này
            SecretKey sessionKey = CryptoUtils.generateAESKey();
            String sessionKeyStr = CryptoUtils.secretKeyToString(sessionKey);

            // 3. Mã hóa Session Key bằng Public Key của đối phương (RSA)
            String encryptedSessionKey = CryptoUtils.encryptRSA(sessionKeyStr, targetPeer.getPublicKey());

            // 4. Tạo Payload Handshake và Ký
            HandshakePayload payload = new HandshakePayload(
                    Cache.getInstance().getCredential().getId(), // My ID
                    encryptedSessionKey
            );
            payload.sign(Cache.getInstance().getCredential().getPrivateKey());

            // 5. Gửi gói tin HANDSHAKE
            NetworkPacket packet = new NetworkPacket(NetworkPacket.PacketType.HANDSHAKE, JsonUtils.toJson(payload));

            // (Gửi raw vì chưa có PeerConnection wrapper)
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            byte[] data = packet.toBytes();
            dos.writeInt(data.length);
            dos.write(data);
            dos.flush();

            // 6. CHỜ PHẢN HỒI (ACK) - Rất quan trọng
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            int len = dis.readInt();
            byte[] buf = new byte[len];
            dis.readFully(buf);

            NetworkPacket responsePacket = NetworkPacket.fromBytes(buf, 0, len);

            if (responsePacket.getPacketType() == NetworkPacket.PacketType.HANDSHAKE_ACK) {
                // Handshake thành công!
                // Tắt timeout để dùng cho chat lâu dài
                String encryptedPayload = responsePacket.getPayload();
                String plainPayload = CryptoUtils.decryptAES(encryptedPayload, sessionKey);
                HandshakeAckPayload handshakeAckPayload;
                try{
                    handshakeAckPayload = JsonUtils.fromJson(plainPayload, HandshakeAckPayload.class);
                    if(handshakeAckPayload == null)
                        throw new Exception("null handshake payload");
                }catch (Exception e){
                    throw e;
                }

                // verify package....
                boolean check = handshakeAckPayload.getSenderId().equals(targetPeer.getId())
                        && handshakeAckPayload.verify(targetPeer.getPublicKey())
                        && (System.currentTimeMillis()-handshakeAckPayload.getTimestamp() < 5000);
                if (! check)
                    throw  new Exception("Verify handshake ack failed");

                if(handshakeAckPayload.isAccept()){
                    return new PeerConnection(targetPeer, socket, sessionKey);
                }else{
                    throw new Exception("Handshake failed: get HandshakeAck.accept = false");
                }

            } else {
                throw new Exception("Handshake failed: Invalid response type " + responsePacket.getPacketType());
            }

        } catch (Exception e) {
//            e.printStackTrace();
            Logger.error(e);
            socket.close(); // Dọn dẹp nếu lỗi
            throw e;
        }
    }

    private void handleDiscoveryUnicast(DatagramPacket packet){
        Logger.info("Get udp unicast discover from " + packet.getSocketAddress());
        NetworkPacket networkPacket = NetworkPacket.fromDatagramPacket(packet);
        if(networkPacket.getPacketType() != NetworkPacket.PacketType.DISCOVER)
            return;
        DiscoveryPayload discoveryPayload = networkPacket.getPayloadAs(DiscoveryPayload.class);
        boolean check = discoveryPayload.verify(discoveryPayload.getPeer().getPublicKey())
                && (System.currentTimeMillis() - discoveryPayload.getTimestamp() < 3000);
        if (!check)
            return;
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
            Cache.getInstance().addPeer(discoveryPayload.getPeer());
            Logger.debug("Found new peer, ask for sync");
            SyncService.requestSyncMetadata(discoveryPayload.getPeer());
        }

    }

    private void handleDiscoveryMulticast(DatagramPacket packet){
        Logger.info("Get udp multicast discover from " + packet.getSocketAddress());
        NetworkPacket networkPacket = NetworkPacket.fromDatagramPacket(packet);
        if(networkPacket.getPacketType() != NetworkPacket.PacketType.DISCOVER)
            return;
        DiscoveryPayload discoveryPayload = networkPacket.getPayloadAs(DiscoveryPayload.class);
        boolean check = discoveryPayload.verify(discoveryPayload.getPeer().getPublicKey())
                && (System.currentTimeMillis() - discoveryPayload.getTimestamp() < 3000);
        if (!check){
            Logger.info("Verify failed");
            Logger.info(discoveryPayload.verify(discoveryPayload.getPeer().getPublicKey()));
            Logger.info((System.currentTimeMillis() - discoveryPayload.getTimestamp() < 3000));
        }

        //fix self broadcast
        if(discoveryPayload.getPeer().getId().compareTo(Cache.getInstance().getCredential().getId()) != 0){
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
                Cache.getInstance().addPeer(discoveryPayload.getPeer());
                Logger.debug("Found new peer, ask for sync");
                SyncService.requestSyncMetadata(discoveryPayload.getPeer());
            }
        }else{
            //self broad cast, ignore
            Logger.info("ignore");
            return;
        }


        // send reply discovery
        try{
            Peer myPeer = Cache.getInstance().getMyPeer();
            if(myPeer == null)
                return;

            DiscoveryPayload discoveryPayloadReply = new DiscoveryPayload(myPeer);
            discoveryPayload.sign(Cache.getInstance().getCredential().getPrivateKey());
            String payload = JsonUtils.toJson(discoveryPayloadReply);
            NetworkPacket replyDiscovery = new NetworkPacket(NetworkPacket.PacketType.DISCOVER, payload);
            this.discoveryService.sendUnicast(replyDiscovery.toBytes(), discoveryPayload.getPeer().getIp(), discoveryPayload.getPeer().getPort());
            Logger.info("Send reply discover ok");
        }catch (IOException e){
//            e.printStackTrace();
            Logger.error(e);
        }
    }

    private void sendDiscoverMulticast(){
        try{
            Logger.info("Send discovery multicast");
            Peer myPeer = Cache.getInstance().getMyPeer();
            if(myPeer == null)
                return;
            DiscoveryPayload discoveryPayload = new DiscoveryPayload(myPeer);
            discoveryPayload.sign(Cache.getInstance().getCredential().getPrivateKey());
            String payload = JsonUtils.toJson(discoveryPayload);
            NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.DISCOVER, payload);
            this.discoveryService.sendMulticast(networkPacket.toBytes());
        }catch (Exception e){
//            e.printStackTrace();
            Logger.error(e);
        }
    }


    static void main() throws UnknownHostException, InterruptedException {
        LogConfig.config();
        Credential credential = new Credential(CryptoUtils.generateRSAKeyPair(), "Tú(window)");
        InetAddress address = InetAddress.getByName("192.168.65.1");
        int port = 9999;
        AuthService.login(credential, address, port);
        Logger.info("Login ok, check cache:");
        Logger.info("Credential: " + JsonUtils.toJson(Cache.getInstance().getCredential()));
        Logger.info("Address" + Cache.getInstance().getIp());
        Logger.info("Port: " + port);

        Logger.info("Start network service");
        NetworkService networkService = new NetworkService(address, port);
        networkService.start();

        while(true){
            Logger.info("Connection pool size : " + ConnectionPool.getInstance().getPoolEntrySet().size());
            Logger.info("Known Peer List size : " + Cache.getInstance().getPeerEntrySet().size());
            for (var entry : Cache.getInstance().getPeerEntrySet()){
                Peer p = entry.getValue();
                Logger.info(JsonUtils.toJson(p));
                CompletableFuture<PeerConnection> future = ConnectionPool.getInstance().getOrConnect(p);
                future
                        .thenAccept(peerConnection -> Logger.info("Get connection ok"))
                        .exceptionally(t->{
                            Logger.info("connect failed");
                            return  null;
                        });
            }
            Thread.sleep(3000);
        }
    }

}
