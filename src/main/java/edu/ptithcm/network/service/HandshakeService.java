package edu.ptithcm.network.service;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Peer;
import edu.ptithcm.network.connection.ConnectionPool;
import edu.ptithcm.network.connection.PeerConnection;
import edu.ptithcm.network.packet.payload.HandshakeAckPayload;
import edu.ptithcm.network.packet.payload.HandshakePayload;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.util.JsonUtils;
import org.tinylog.Logger;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandshakeService {

    private volatile boolean running;
    private final InetAddress bindAddress;
    private final int bindPort;
    private ServerSocket serverSocket;
    private ExecutorService executor;


    public HandshakeService(InetAddress bindAddress, int bindPort){
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
    }

    public void start() {
        if(running)
            return;
        running = true;
        executor = Executors.newVirtualThreadPerTaskExecutor();
        try{
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(bindAddress, bindPort));
        }catch (IOException e){
            stop();
        }

        executor.execute(this::listenTcp);
        Logger.info("HandShake Service(TCP listener) start on " + bindAddress + ":" + bindPort);
    }

    public void stop(){
        running = false;
        try{
            if(serverSocket != null)
                serverSocket.close();
        }catch (Exception e){}
        if(executor != null)
            executor.close();
        Logger.info("HandShake Service(TCP listener) stopped.");
    }

    public void listenTcp(){
        try{
            while (running){
                Socket client = serverSocket.accept();
                HandshakeService.handleIncomingHandshake(client);
            }
        }catch (IOException e){
            if (running)
                Logger.error(e);
        }
    }


    /**
     * <pre>
     *     Thực hiện gởi yêu cầu handshake
     *     Tạo session key, gởi gói handshake
     *     Nhận và check handshake ack, nếu verify ok và ack ==  accept thì trả về PeerConnection
     *     Việc thêm vào connection pool được ConnectionPool xử lý
     *
     * </pre>
     * @param targetPeer
     * @return
     * @throws Exception Nếu không thành công, tự động đóng socket
     */
    public static PeerConnection performOutgoingHandshake(Peer targetPeer) throws Exception {
        Logger.debug("Make OutComingHandshake request to " + targetPeer.getIp());

        // 1. Create socket
        Socket socket = new Socket(targetPeer.getIp(), targetPeer.getPort());
        socket.setSoTimeout(5000);
        try {
            // 2. Generate SessionKey AES
            SecretKey sessionKey = CryptoUtils.generateAESKey();
            String sessionKeyStr = CryptoUtils.secretKeyToString(sessionKey);

            // 3. Encrypt Session key by targetPeer PublicKey
            String encryptedSessionKey = CryptoUtils.encryptRSA(sessionKeyStr, targetPeer.getPublicKey());

            // 4. Generate Payload + Sign
            HandshakePayload payload = new HandshakePayload(
                    Cache.getInstance().getCredential().getId(), // My ID
                    encryptedSessionKey
            );
            payload.sign(Cache.getInstance().getCredential().getPrivateKey());

            // 5. Send Handshake request
            NetworkPacket packet = new NetworkPacket(NetworkPacket.PacketType.HANDSHAKE, JsonUtils.toJson(payload));

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            byte[] data = packet.toBytes();
            dos.writeInt(data.length);
            dos.write(data);
            dos.flush();

            // 6. Wait for HandShake ACK
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            int len = dis.readInt();
            byte[] buf = new byte[len];
            dis.readFully(buf);

            NetworkPacket responsePacket = NetworkPacket.fromBytes(buf, 0, len);
            if(responsePacket.getPacketType() != NetworkPacket.PacketType.HANDSHAKE_ACK){
                Logger.warn("Invalid network packet type for handshake ack");
                throw new Exception("Invalid network packet type for handshake ack");
            }

            String encryptedPayload = responsePacket.getPayload();
            String plainPayload = CryptoUtils.decryptAES(encryptedPayload, sessionKey);
            HandshakeAckPayload handshakeAckPayload =JsonUtils.fromJson(plainPayload, HandshakeAckPayload.class);
            if(handshakeAckPayload == null){
                Logger.warn("Invalid Handshake Ack Payload : null payload");
                throw new Exception("null handshake payload");
            }

            // verify package....
            boolean check = handshakeAckPayload.getSenderId().equals(targetPeer.getId())
                    && handshakeAckPayload.verify(targetPeer.getPublicKey())
                    && (System.currentTimeMillis()-handshakeAckPayload.getTimestamp() < 5000);
            if (! check){
                Logger.warn("Handshake Ack verify failed");
                throw  new Exception("Verify handshake ack failed");
            }


            if(handshakeAckPayload.isAccept()){
                // infinite timeout
                socket.setSoTimeout(0);
                return new PeerConnection(targetPeer, socket, sessionKey);
            }else{
                throw new Exception("Handshake failed: get HandshakeAck.accept = false");
            }

        } catch (Exception e) {
            Logger.error(e);
            socket.close(); // Dọn dẹp nếu lỗi
            throw e;
        }
    }

    /**
     * Serversocket nhận đc yêu cầu handshake từ máy khác
     * Đọc gói tin handshake
     * Kiểm tra chữ ký số, thử thêm vào pool
     * Logic xử lý concurrency handshake được xử lý bên ConnectionPool, gọi hàm addIncomingConnection là được
     * @param client
     */
    private static void handleIncomingHandshake(Socket client){
        try{
            // Read Payload
            DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
            int length = dataInputStream.readInt();
            byte[] buf = new byte[length];
            dataInputStream.readFully(buf);
            NetworkPacket networkPacket = NetworkPacket.fromBytes(buf, 0, buf.length);
            if(networkPacket.getPacketType() != NetworkPacket.PacketType.HANDSHAKE){
                Logger.warn("Reject Incoming Handshake: invalid network packet type");
                client.close();
                return;
            }
            HandshakePayload handshakePayload = networkPacket.getPayloadAs(HandshakePayload.class);

            // Verify
            Peer senderPeer = Cache.getInstance().getPeer(handshakePayload.getSenderId());
            boolean check = (senderPeer != null)
                    && senderPeer.getId().equals(CryptoUtils.hashSHA256(CryptoUtils.publicKeyToString(senderPeer.getPublicKey())))
                    && (System.currentTimeMillis() - handshakePayload.getTimestamp() < 10000)
                    && (handshakePayload.verify(senderPeer.getPublicKey()));
            if (!check){
                client.close();
                Logger.warn("Reject Incoming Handshake: verify failed");
                return;
            }


            // Get Session AES key
            String encryptedSessionKeyStr = handshakePayload.getEncryptedSessionKey();
            String plainSessionKeyStr = CryptoUtils.decryptRSA(encryptedSessionKeyStr, Cache.getInstance().getCredential().getPrivateKey());
            if(plainSessionKeyStr == null){
                client.close();
                Logger.warn("Reject Incoming Handshake: no session key provided");
                return;
            }
            SecretKey sessionKey = CryptoUtils.stringToSecretKey(plainSessionKeyStr);


            // Try to create Connection
            boolean addSuccess = ConnectionPool.getInstance().addIncomingConnection(senderPeer, client, sessionKey);


            // Send reply
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
                Logger.debug(String.format("Incoming Handshake from %s reject by ConnectionPool", client.getRemoteSocketAddress()));
            }else{
                Logger.debug(String.format("Incoming Handshake from %s accepted by ConnectionPool", client.getRemoteSocketAddress()));
            }
        } catch (Exception e) {
            try{client.close();}catch (Exception ee){}
        }

    }
}
