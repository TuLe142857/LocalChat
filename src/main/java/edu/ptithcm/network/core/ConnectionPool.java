package edu.ptithcm.network.core;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ConnectionPool {
    private static final ConnectionPool instance = new ConnectionPool();

    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService virtualExecutorService;
    private final ConcurrentHashMap<String, PeerConnection> pool;

    private ConnectionPool(){
        pool = new ConcurrentHashMap<>();
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        virtualExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static ConnectionPool getInstance(){
        return  instance;
    }

    public boolean addConnection(PeerConnection connection){
        return false;
    }

    public void removeConnection(String id){

    }

    public void clear(){

    }
}
