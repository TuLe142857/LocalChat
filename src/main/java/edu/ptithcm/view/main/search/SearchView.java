package edu.ptithcm.view.main.search;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.PeerDiscoveryEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Peer;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.util.function.Consumer;
import java.util.List;
import org.tinylog.Logger;


public class SearchView extends BaseView {
    private ListView<Peer> peerListView;
    private ObservableList<Peer> availablePeers;
    private final Consumer<Peer> chatStarter;
    private Runnable unsubscribeRunnable;

    // Constructor sử dụng Consumer<Peer>
    public SearchView(Consumer<Peer> chatStarter) {
        this.chatStarter = chatStarter;
    }

    // Default constructor (chỉ cho tính tương thích nếu cần)
    public SearchView() {
        this.chatStarter = p -> {};
    }


    @Override
    protected void init() {
        availablePeers = FXCollections.observableArrayList();
    }

    @Override
    protected void setupUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f7f7f7;");

        Label title = new Label("🔎 Peer đang hoạt động (Đã Khám phá)");
        title.setStyle("-fx-font-size: 1.5em; -fx-padding: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");

        peerListView = new ListView<>(availablePeers);
        peerListView.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc;");

        peerListView.setCellFactory(lv -> new PeerListCell(chatStarter));

        root.setTop(title);
        root.setCenter(peerListView);

        this.getChildren().add(root);
    }

    // PeerListCell
    private static class PeerListCell extends ListCell<Peer> {
        private final Consumer<Peer> chatStarter;
        private final BorderPane pane = new BorderPane();
        private final Label nameLabel = new Label();
        private final Label ipLabel = new Label();
        private final Button chatButton = new Button("Nhắn tin");

        public PeerListCell(Consumer<Peer> chatStarter) {
            this.chatStarter = chatStarter;

            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em; -fx-text-fill: #3f51b5;");
            ipLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #777;");

            VBox infoBox = new VBox(5, nameLabel, ipLabel);

            chatButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5 15 5 15; -fx-cursor: hand; -fx-background-radius: 5;");

            pane.setLeft(infoBox);
            pane.setRight(chatButton);
            pane.setPadding(new Insets(10));
            pane.setStyle("-fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-background-color: white;");
        }

        @Override
        protected void updateItem(Peer peer, boolean empty) {
            super.updateItem(peer, empty);
            if (empty || peer == null) {
                setGraphic(null);
                setText(null);
            } else {
                String myId = Cache.getInstance().getCredential() != null
                        ? Cache.getInstance().getCredential().getId()
                        : null;

                if (peer.getId().equals(myId)) {
                    nameLabel.setText(peer.getName() + " (Bạn)");
                    ipLabel.setText(peer.getIp().getHostAddress());
                    setGraphic(new VBox(10, nameLabel, ipLabel));
                    setDisable(true);
                    return;
                }

                nameLabel.setText(peer.getName());
                ipLabel.setText("IP: " + peer.getIp().getHostAddress() + ", Port: " + peer.getPort());

                // Kích hoạt chat qua callback
                chatButton.setOnAction(e -> chatStarter.accept(peer));
                setGraphic(pane);
                setDisable(false);
            }
        }
    }


    @Override
    public void loadData() {
        // Lấy dữ liệu khởi tạo lần đầu từ Cache
        availablePeers.clear();

        // Sử dụng getPeerList()
        List<Peer> peers = Cache.getInstance().getPeerList();

        // Lọc Peer chính mình
        String myId = Cache.getInstance().getCredential() != null ? Cache.getInstance().getCredential().getId() : null;
        if(myId != null) peers.removeIf(peer -> peer.getId().equals(myId));

        availablePeers.addAll(peers);
    }

    @Override
    public void setupEventBus() {
        // Đăng ký lắng nghe sự kiện PeerDiscoveryEvent
        if (this.unsubscribeRunnable == null) {
            this.unsubscribeRunnable = MessageBus.subscribe(PeerDiscoveryEvent.class, this::handlePeerDiscovery);
        }
    }

    // Xử lý sự kiện PeerDiscoveryEvent
    private void handlePeerDiscovery(PeerDiscoveryEvent event) {
        // Bắt buộc chạy trên JavaFX Application Thread để cập nhật UI
        Platform.runLater(() -> {
            loadData(); // Tải lại toàn bộ danh sách khi có Peer mới
        });
    }

    @Override
    public void onRemove() {
        super.onRemove();
        // Gọi run() trên đối tượng Runnable đã lưu
        if (this.unsubscribeRunnable != null) {
            this.unsubscribeRunnable.run();
            this.unsubscribeRunnable = null;
        }
    }
}