package edu.ptithcm.bus.event;

import edu.ptithcm.model.Peer;

import java.util.Collection;
import java.util.Collections;

public class PeerDiscoveryEvent extends AppEvent {
    private final Collection<Peer> discoveredPeers;

    public PeerDiscoveryEvent(Collection<Peer> discoveredPeers) {
        // Truyền một bản sao không thể chỉnh sửa để bảo vệ dữ liệu cache
        this.discoveredPeers = Collections.unmodifiableCollection(discoveredPeers);
    }

    public Collection<Peer> getDiscoveredPeers() {
        return discoveredPeers;
    }
}