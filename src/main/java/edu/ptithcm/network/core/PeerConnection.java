package edu.ptithcm.network.core;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Message;
import edu.ptithcm.network.packet.MessageAckPayload;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.model.Peer;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.util.JsonUtils;
import org.tinylog.Logger;
import edu.ptithcm.network.packet.MessagePayload; // Import mới

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerConnection {

    private volatile boolean running;
    private final Peer peer;
    private final Socket socket;
    private final DataOutputStream dataOutputStream;
    private final DataInputStream dataInputStream;
    private final SecretKey sessionKey;
    private volatile long lastHeartbeat;
    private final ExecutorService executor;

    public PeerConnection(Peer peer, Socket socket, SecretKey sessionKey) throws IOException {
        Logger.debug(
                String.format(
                        "New PeerConnection created: peerId: %s localSocketIP: %s, remoteSocketIP: %s",
                        peer.getId(),
                        socket.getLocalSocketAddress().toString(),
                        socket.getRemoteSocketAddress().toString())
        );
        this.peer = peer;
        this.socket = socket;
        this.sessionKey = sessionKey;
        this.dataInputStream = new DataInputStream(socket.getInputStream());
        this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        this.lastHeartbeat = System.currentTimeMillis();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.execute(this::listen);
    }

    // [MODIFIED]: sendNetworkPacket để đảm bảo flush
    public synchronized void sendNetworkPacket(NetworkPacket packet) throws IOException {
        Logger.debug(
                String.format(
                        "Send NetworkPacket type = %s to %s through %s",
                        packet.getPacketType().toString(),
                        socket.getRemoteSocketAddress().toString(),
                        socket.getLocalSocketAddress().toString())
        );
        String plainJson = JsonUtils.toJson(packet);
        String encryptedJson = CryptoUtils.encryptAES(plainJson, this.sessionKey);
        if(encryptedJson == null)
            throw new RuntimeException("Encryption failed.");

        byte []buf = encryptedJson.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeInt(buf.length);
        dataOutputStream.write(buf);
        dataOutputStream.flush(); // Ensure data is sent immediately
    }

    // [NEW METHOD]: Handle incoming NetworkPacket and decrypt
    private NetworkPacket receiveNetworkPacket() throws IOException {
        int length = dataInputStream.readInt();
        if (length <= 0) {
            Logger.error("Invalid length of network packet received: " + length);
            throw new IOException("Invalid packet length");
        }

        byte [] buf = new byte[length];
        dataInputStream.readFully(buf);

        String encryptedJson = new String(buf, StandardCharsets.UTF_8);
        String plainJson = CryptoUtils.decryptAES(encryptedJson, this.sessionKey);
        if (plainJson == null) {
            Logger.error("Failed to decrypt incoming packet.");
            throw new IOException("Decryption failed");
        }

        NetworkPacket networkPacket = JsonUtils.fromJson(plainJson, NetworkPacket.class);
        return networkPacket;
    }


    // when get a message, send to message bus
    private void listen(){
        running = true;
        try{
            while(running){
                NetworkPacket networkPacket = receiveNetworkPacket();

                Logger.debug(
                        String.format(
                                "Get NetworkPacket type = %s from %s through %s",
                                networkPacket.getPacketType().toString(),
                                socket.getRemoteSocketAddress().toString(),
                                socket.getLocalSocketAddress().toString()
                        )
                );

                if(networkPacket.getPacketType() == NetworkPacket.PacketType.HEART_BEAT){
                    this.lastHeartbeat = System.currentTimeMillis();
                }else if(networkPacket.getPacketType() == NetworkPacket.PacketType.MESSAGE){
                    // SỬA: Dùng MessagePayload để đọc gói tin
                    MessagePayload payload = networkPacket.getPayloadAs(MessagePayload.class);

                    // 1. Tạo đối tượng Message từ Payload
                    // ConversationId của tin nhắn Direct Chat đến (Host 2) là ID của Host 1 (Peer đối tác)
                    Message message = new Message(
                            peer.getId(), // Conversation ID = Peer ID của đối tác
                            payload.getSenderId(),
                            payload.getContent(),
                            payload.getLamportClock()
                    );

                    // 2. Phát sự kiện cho ChatService xử lý (tạo Conversation và thêm Message)
                    MessageBus.emit(new MessageReceivedEvent(message));

                    // 3. Gửi lại ACK
                    MessageAckPayload messageAckPayload = new MessageAckPayload(peer.getId(), Cache.getInstance().getCredential().getId(), message.getLamportClock());
                    NetworkPacket ackPacket = new NetworkPacket(NetworkPacket.PacketType.MESSAGE_ACK, JsonUtils.toJson(messageAckPayload));
                    sendNetworkPacket(ackPacket);

                }else if(networkPacket.getPacketType() == NetworkPacket.PacketType.MESSAGE_ACK){
                    // SỬA: Dùng MessageAckPayload để đọc gói tin
                    MessageAckPayload payload = networkPacket.getPayloadAs(MessageAckPayload.class);
                    // Phát sự kiện cho ChatService xử lý (tìm Message và cập nhật trạng thái)
                    MessageBus.emit(new MessageSendSuccessEvent(payload.getLamportClock(), payload.getConversationId()));
                }else{
                    Logger.warn("Unexpected NetworkPacket type to PeerConnection " + peer.getId() +" : " + networkPacket.getPacketType());
                }
            }
        }
        catch (EOFException | SocketException e){
            if (running) {
                Logger.info("Connection closed by peer: " + peer.getName());
                // Gọi ConnectionPool remove để dọn dẹp
                ConnectionPool.getInstance().removeConnection(peer.getId());
            }
        }
        catch (Exception e){
            if(running)
                Logger.error(e);
        }

    }

    public void close(){
        try{
            running = false;
            socket.close();
            this.executor.close();
        }catch (Exception e){}
    }

    public Peer getPeer() {
        return peer;
    }

    public Socket getSocket() {
        return socket;
    }

    public SecretKey getSessionKey() {
        return sessionKey;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
}