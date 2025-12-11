package edu.ptithcm.network.core;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
import edu.ptithcm.model.Credential;
import edu.ptithcm.model.Message;
import edu.ptithcm.network.packet.MessageAckPayload;
import edu.ptithcm.network.packet.NetworkPacket;
import edu.ptithcm.model.Peer;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.util.JsonUtils;

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
    private Peer peer;
    private final Socket socket;
    private final DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private final SecretKey sessionKey;
    private volatile long lastHeartbeat;
    private ExecutorService executor;

    public PeerConnection(Peer peer, Socket socket, SecretKey sessionKey) throws IOException {
        this.peer = peer;
        this.socket = socket;
        this.sessionKey = sessionKey;
        this.dataInputStream = new DataInputStream(socket.getInputStream());
        this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        this.lastHeartbeat = System.currentTimeMillis();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.execute(this::listen);
    }

    //encypt & send
    public synchronized void sendNetworkPacket(NetworkPacket packet) throws IOException {
        String plainJson = JsonUtils.toJson(packet);
        String encryptedJson = CryptoUtils.encryptAES(plainJson, this.sessionKey);
        if(encryptedJson == null)
            throw new RuntimeException("How ???????");

        byte []buf =encryptedJson.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeInt(buf.length);
        dataOutputStream.write(buf);
    }

    // when get a message, send to message bus
    private void listen(){
        try{
            while(running){
                int length = dataInputStream.readInt();
                if (length < 0) throw new IOException("Invalid length");
                byte [] buf = new byte[length];
                dataInputStream.readFully(buf);

                String encryptedJson = new String(buf, StandardCharsets.UTF_8);
                String plainJson = CryptoUtils.decryptAES(encryptedJson, this.sessionKey);
                NetworkPacket networkPacket = JsonUtils.fromJson(plainJson, NetworkPacket.class);

                if(networkPacket.getPacketType() == NetworkPacket.PacketType.HEART_BEAT){
                    this.lastHeartbeat = System.currentTimeMillis();
                }else if(networkPacket.getPacketType() == NetworkPacket.PacketType.MESSAGE){
                    Message message = networkPacket.getPayloadAs(Message.class);
                    MessageBus.emit(new MessageReceivedEvent(message));
                }else if(networkPacket.getPacketType() == NetworkPacket.PacketType.MESSAGE_ACK){
                    MessageAckPayload messageAckPayload = networkPacket.getPayloadAs(MessageAckPayload.class);
                    MessageBus.emit(new MessageSendSuccessEvent(messageAckPayload.getMessageId(), messageAckPayload.getConversationId()));
                }else{
                    IO.println("Unexpected NetworkPacket type to PeerConnection " + peer.getId() +" : " + networkPacket.getPacketType());
                }
            }
        }
        catch (EOFException | SocketException e){
            if (running) {
                IO.println("Connection closed by peer: " + peer.getName());
                // Gọi ConnectionPool remove để dọn dẹp
                ConnectionPool.getInstance().removeConnection(peer.getId());
            }
        }
        catch (Exception e){
            if(running)
                e.printStackTrace();
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
