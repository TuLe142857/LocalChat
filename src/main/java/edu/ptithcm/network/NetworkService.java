package edu.ptithcm.network;

import module java.base;
import edu.ptithcm.network.service.DiscoveryService;
import edu.ptithcm.network.service.HandshakeService;
import org.tinylog.Logger;

public class NetworkService {

    // Default Config for UDP Discover
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


    private HandshakeService handshakeService;
    private DiscoveryService discoveryService;
    private final InetAddress bindAddress;
    private final int tcpPort;

    public NetworkService(InetAddress bindAddress, int tcpPort){
        this.bindAddress = bindAddress;
        this.tcpPort = tcpPort;
    }

    public void start(){
        handshakeService = new HandshakeService(bindAddress, tcpPort);
        discoveryService = new DiscoveryService(
                discoveryUnicastPort,
                discoveryMulticastPort,
                discoveryMulticastGroup,
                bindAddress
        );

        //start service
        Logger.info("Network service start...");
        handshakeService.start();
        discoveryService.start();
    }

    public void stop(){
        handshakeService.stop();
        discoveryService.stop();
        Logger.info("Network service stopped.");
    }
}
