package edu.ptithcm.network;

import module java.base;
import edu.ptithcm.model.NetworkPacket;
import edu.ptithcm.network.listener.TcpListener;
import edu.ptithcm.network.listener.UdpListener;
import edu.ptithcm.util.JsonUtils;

public class NetworkService {
    private ExecutorService executor;
    private UdpListener udpListener;
    private TcpListener tcpListener;
    private final InetAddress bindAddress;
    private final int tcpPort;

    public NetworkService(InetAddress bindAddress, int tcpPort){
        this.bindAddress = bindAddress;
        this.tcpPort = tcpPort;
    }

    public void start(){
        tcpListener = new TcpListener(bindAddress, tcpPort, this::handleTcpClient);
        udpListener = new UdpListener(bindAddress, 9999, this::handleUdpClient);

        // init thread pool
        executor = Executors.newVirtualThreadPerTaskExecutor();

        //start thread
        executor.execute(()->tcpListener.start());
        executor.execute(()->udpListener.start());
    }

    public void stop(){
        IO.println("Network service stop");
        udpListener.stop();
        tcpListener.stop();
        executor.close();
    }

    private void handleTcpClient(Socket client){
        try{
            DataInputStream in = new DataInputStream(client.getInputStream());
            int length = in.readInt();
            byte[] buf = new byte[length];
            in.readFully(buf);
            String json = new String(buf, StandardCharsets.UTF_8);
            NetworkPacket packet = NetworkPacket.fromBytes(buf, 0, buf.length);

            IO.println("TCP From " + client.getRemoteSocketAddress());
            IO.println(json);
            IO.println(JsonUtils.toJson(packet));
            IO.println();
        } catch (Exception e) {
//            throw new RuntimeException(e);
        }

    }

    private void handleUdpClient(DatagramPacket ppacket){
//        byte [] buf = ppacket.getData();
//        String json = new String(buf, 0, ppacket.getLength(), StandardCharsets.UTF_8);
//        NetworkPacket packet = JsonUtils.fromJson(json, NetworkPacket.class);

        NetworkPacket packet = NetworkPacket.fromDatagramPacket(ppacket);
        IO.println("UCP From " + ppacket.getSocketAddress());
        IO.println(JsonUtils.toJson(packet));
        IO.println(JsonUtils.toJson(packet));
        IO.println();
    }

    static void main() throws UnknownHostException {
        NetworkService networkService = new NetworkService(InetAddress.getByName("192.168.65.1"), 9999);
        networkService.start();
        ScheduledExecutorService exe = Executors.newScheduledThreadPool(2);
        exe.scheduleWithFixedDelay(()->{
            try(Socket socket = new Socket(InetAddress.getByName("192.168.65.1"), 9999);){
//                IO.println("send");
                NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.DISCOVER, "id",  "discover", "sig");
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                byte [] buf = networkPacket.toBytes();
                dos.writeInt(buf.length);
                dos.write(buf);
                dos.flush();
            }catch (Exception e){}
        },0,3,TimeUnit.SECONDS);

        exe.scheduleWithFixedDelay(()->{
            try(DatagramSocket socket = new DatagramSocket()){
                NetworkPacket networkPacket = new NetworkPacket(NetworkPacket.PacketType.DISCOVER, "id",  "discover", "sig");
                byte [] buf = JsonUtils.toJson(networkPacket).getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("192.168.65.1"), 9999);
                socket.send(packet);
            }catch (Exception e){};
        }, 0, 3, TimeUnit.SECONDS);
    }


}
