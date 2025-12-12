package edu.ptithcm.network;

import java.net.UnknownHostException;
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
import edu.ptithcm.util.JsonUtils;
import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.PeerDiscoveryEvent;
import edu.ptithcm.util.LogConfig;
import org.tinylog.Logger;
import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NetworkService {

    // --- Singleton Pattern ---
    private static NetworkService instance;
    public static NetworkService getInstance() {
        if (instance == null) {
            // Nên được khởi tạo khi Login
            // Thường là: new NetworkService(ip, port)
            throw new IllegalStateException("NetworkService not initialized. Call constructor first.");
        }
        return instance;
    }

    // --- Configuration ---
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

    // --- Services ---
    private ScheduledExecutorService scheduledExecutorService;
    private HandshakeService handshakeService;
    private DiscoveryService discoveryService;
    private final InetAddress bindAddress;
    private final int tcpPort;

    public NetworkService(InetAddress bindAddress, int tcpPort){
        this.bindAddress = bindAddress;
        this.tcpPort = tcpPort;
        if (instance != null) {
            Logger.warn("NetworkService already initialized. Overwriting instance.");
        }
        instance = this; // Set Singleton instance
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
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        if (handshakeService != null) {
            handshakeService.stop();
        }
        if (discoveryService != null) {
            discoveryService.stop();
        }
    }

    /**
     * Serversocket nhận đc yêu cầu handshake từ máy khác
     * Đọc gói tin handshake
     * Kiểm tra
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
            Logger.error(e, "Error handling incoming handshake client from: " + client.getInetAddress());
            try{client.close();}catch (Exception ee){}
        }

    }

    /**
     * <pre>
     * Thực hiện gởi yêu cầu handshake
     * Tạo session key, gởi gói handshake
     * Nhận và check handshake ack, nếu verify ok và ack ==  accept thì trả về PeerConnection
     * Việc thêm vào connection pool phải do luồng gọi hàm này tự xủ lý
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
                socket.setSoTimeout(0);

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
            socket.close(); // Dọn dẹp nếu lỗi
            throw e;
        }
    }

    public void registerPacketHandler(java.util.function.Consumer<NetworkPacket> handler) {
        // Hiện tại PeerConnection xử lý MESSAGE và MESSAGE_ACK, nhưng nếu cần các gói tin khác
        // xử lý ở NetworkService thì có thể thêm logic ở đây.
        // Tuy nhiên, vì logic MESSAGE đã được chuyển vào PeerConnection và ChatService (qua MessageBus),
        // ta không cần thêm logic ở đây.
    }

    // Giữ nguyên các hàm Discovery
    private void handleDiscoveryUnicast(DatagramPacket packet){
        // ... (Giữ nguyên logic cũ)
    }

    private void handleDiscoveryMulticast(DatagramPacket packet){
        // ... (Giữ nguyên logic cũ)
    }

    private void sendDiscoverMulticast(){
        // ... (Giữ nguyên logic cũ)
    }

    static void main() throws UnknownHostException, InterruptedException {
        // ... (Giữ nguyên logic cũ)
    }

}