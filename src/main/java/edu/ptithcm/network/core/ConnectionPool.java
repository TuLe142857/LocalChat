package edu.ptithcm.network.core;


import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Peer;
import edu.ptithcm.network.NetworkService;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class ConnectionPool {
    private static final ConnectionPool instance = new ConnectionPool();


    private final ConcurrentHashMap<String, PeerConnection> pool;
    private final ConcurrentHashMap<String, CompletableFuture<PeerConnection>> pendingConnections;

    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService virtualThreadExecutor;


    private ConnectionPool(){
        pool = new ConcurrentHashMap<>();
        pendingConnections = new ConcurrentHashMap<>();
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
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

    /**
     * Hàm quan trọng nhất: Lấy kết nối có sẵn HOẶC tự mở kết nối mới.
     * Hàm này KHÔNG BLOCK (trả về Future).
     */
    public CompletableFuture<PeerConnection> getOrConnect(Peer targetPeer) {
        // 1. Kiểm tra xem đã có kết nối sẵn chưa
        if (pool.containsKey(targetPeer.getId())) {
            return CompletableFuture.completedFuture(pool.get(targetPeer.getId()));
        }

        // 2. Sử dụng computeIfAbsent để đảm bảo chỉ CÓ 1 THREAD được quyền khởi tạo kết nối
        return pendingConnections.computeIfAbsent(targetPeer.getId(), peerId -> {

            // Tạo một Future để trả về cho UI
            CompletableFuture<PeerConnection> future = new CompletableFuture<>();

            // Chạy logic bắt tay trên Virtual Thread (Java 25)
            virtualThreadExecutor.execute(() -> {
                try {
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException("Handshake cancelled");
                    // Gọi logic thực hiện handshake (được implement ở NetworkService hoặc tách ra class riêng)
                    PeerConnection newConn = NetworkService.performOutgoingHandshake(targetPeer);

                    // Handshake thành công
                    pool.put(peerId, newConn);
                    future.complete(newConn);

                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    // Dù thành công hay thất bại, xóa khỏi pending để lần sau thử lại
                    pendingConnections.remove(peerId);
                }
            });

            return future;
        });
    }

    public boolean addIncomingConnection(Peer peer, Socket socket, SecretKey sessionKey) {
        if (pool.containsKey(peer.getId())) {
            // Đã có kết nối rồi -> từ chối kết nối mới này
            // (Hoặc logic phức tạp hơn: so sánh ID để quyết định giữ cái nào)
            return false;
        }

        // Kiểm tra xem mình có đang cố connect tới nó không?
        String remoteId = peer.getId();
        String myId = Cache.getInstance().getCredential().getId();
        if (pendingConnections.containsKey(remoteId)) {
            // Xảy ra va chạm!
            // Giải quyết: So sánh ID
            if (myId.compareTo(remoteId) < 0) {
                // ID mình nhỏ hơn -> Mình là "Boss", kết nối outgoing của mình sẽ thắng.
                // Từ chối incoming này.
                return false;
            } else {
                // ID mình lớn hơn -> Chấp nhận incoming này.
                // Và (quan trọng) phải CANCEL cái pending outgoing kia đi (nếu có thể)
                // Hoặc cứ để outgoing fail/hoàn thành sau rồi check lại pool.
            }
        }

        PeerConnection newIncomingConn;
        try {
            newIncomingConn = new PeerConnection(peer, socket, sessionKey);
        } catch (IOException e) { return false; }

        // putIfAbsent return null if key do not exit and add key-value
        PeerConnection existing = pool.putIfAbsent(peer.getId(), newIncomingConn);
        if (existing != null) {
            // Đã có thread khác nhanh tay hơn thêm vào pool
            newIncomingConn.close();
            return false;
        }

        // [QUAN TRỌNG] Xử lý Pending Future (nếu có)
        // Nếu UI đang chờ kết nối tới peer này (getOrConnect), hãy báo cho nó biết là xong rồi!
        // Dùng chính kết nối Incoming này để complete cho cái Future đó.
        CompletableFuture<PeerConnection> pendingFuture = pendingConnections.remove(remoteId);
        if (pendingFuture != null) {
            // Hủy task outgoing (nếu có thể interrupt)
            pendingFuture.complete(newIncomingConn);
            // Lưu ý: Thread outgoing trong performOutgoingHandshake vẫn sẽ chạy tiếp
            // cho đến khi socket timeout hoặc connect xong, nhưng kết quả của nó sẽ bị bỏ qua
            // vì pendingConnections đã bị remove.
        }

        return true;
    }

    public void removeConnection(String peerId){
        PeerConnection conn = pool.remove(peerId);
        if(conn != null)
            conn.close();
    }

    public void clear(){

        // 1. Close all active connections
        for(var entry : pool.entrySet()){
            entry.getValue().close();
        }
        this.pool.clear();

        // 2. cancel all handshake in progress
        for (var entry : pendingConnections.entrySet()) {
            CompletableFuture<PeerConnection> f = entry.getValue();
            f.cancel(true); // interrupt virtual thread
        }
        pendingConnections.clear();

        // Không đóng virtualExecutor vì sẽ gây lỗi cho hàm getOrCreate không execute đc thread

    }

    public Set<Map.Entry<String, PeerConnection>> getPoolEntrySet(){
        return pool.entrySet();
    }

}
