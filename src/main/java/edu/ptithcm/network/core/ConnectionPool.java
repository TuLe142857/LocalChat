package edu.ptithcm.network.core;


import edu.ptithcm.model.Peer;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class ConnectionPool {
    private static final ConnectionPool instance = new ConnectionPool();

    private final ScheduledExecutorService scheduledExecutorService;
    private final ConcurrentHashMap<String, PeerConnection> pool;

    private ConnectionPool(){
        pool = new ConcurrentHashMap<>();

        //daemon thread pool
        scheduledExecutorService = Executors.newScheduledThreadPool(
                2,
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                }
        );
        scheduledExecutorService.scheduleWithFixedDelay(this::scanAndRemoveConnection, 0, 5, TimeUnit.SECONDS);

    }
    private void scanAndRemoveConnection(){

    }

    public static ConnectionPool getInstance(){
        return  instance;
    }

    public PeerConnection getConnection(String peerId){
        return pool.get(peerId);
    }

    public boolean addConnection(Peer peer, Socket socket, SecretKey sessionKey) {
        if(! pool.containsKey(peer.getId())){
            try{
                pool.putIfAbsent(peer.getId(), new PeerConnection(peer, socket, sessionKey));
                return true;
            }catch (Exception e){
                return false;
            }

        }else{
            // Xử lý trùng(2 bên cùng gởi request handshake)
            return false;
        }
    }

    public void removeConnection(String peerId){
        pool.remove(peerId);
    }

    public void clear(){
        this.pool.clear();
    }

}
