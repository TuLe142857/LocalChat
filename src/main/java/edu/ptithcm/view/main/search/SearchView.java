package edu.ptithcm.view.main.search;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation; // ĐÃ THÊM IMPORT NÀY
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.Peer;
import edu.ptithcm.service.ChatService;
import edu.ptithcm.view.base.BaseView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert.AlertType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SearchView extends BaseView {

    private ListView<Peer> peerListView;
    private ObservableList<Peer> peers;
    private Button startChatButton;
    private Button createGroupButton;

    @Override
    protected void init() {
        peers = FXCollections.observableArrayList();
    }

    @Override
    protected void setupUI() {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        Label title = new Label("Search Peer & Conversation Management");
        title.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");

        // Center: Peer List
        peerListView = new ListView<>(peers);
        peerListView.setCellFactory(param -> new PeerListItem());
        peerListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); // Cho phép chọn nhiều để tạo nhóm

        // Bottom: Action Buttons
        startChatButton = new Button("Start Direct Chat (Chọn 1 Peer)");
        startChatButton.setMaxWidth(Double.MAX_VALUE);
        startChatButton.setOnAction(e -> startDirectChat());

        createGroupButton = new Button("Create Group Chat (Chọn 2+ Peers)");
        createGroupButton.setMaxWidth(Double.MAX_VALUE);
        createGroupButton.setOnAction(e -> showGroupCreationDialog());

        VBox buttonBox = new VBox(10, startChatButton, createGroupButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        // Layout assembly
        layout.setTop(title);
        layout.setCenter(peerListView);
        layout.setBottom(buttonBox);

        this.getChildren().add(layout);
    }

    @Override
    public void loadData() {
        // Load known peers from cache (excluding self)
        List<Peer> peerList = Cache.getInstance().getPeerList();
        peerList.removeIf(p -> p.getId().equals(Cache.getInstance().getCredential().getId()));
        peers.setAll(peerList);
    }

    @Override
    public void setupEventBus() {
        // Logic refresh Peer List (Cần subscribe MessageBus cho event Peer Discovered/Updated nếu có)
    }

    private void startDirectChat() {
        ObservableList<Peer> selectedPeers = peerListView.getSelectionModel().getSelectedItems();

        if (selectedPeers.size() != 1) {
            new Alert(AlertType.WARNING, "Vui lòng chọn chính xác một peer để bắt đầu trò chuyện trực tiếp.").showAndWait();
            return;
        }

        Peer targetPeer = selectedPeers.get(0);
        Conversation existingConv = Cache.getInstance().getConversation(targetPeer.getId());

        if (existingConv == null) {
            // Tạo cuộc trò chuyện trực tiếp mới và thêm vào cache
            DirectConversation newConv = new DirectConversation(targetPeer);
            Cache.getInstance().addConversation(newConv);
            new Alert(AlertType.INFORMATION, "Đã tạo Direct Chat với " + targetPeer.getName() + ".").showAndWait();
        } else {
            new Alert(AlertType.INFORMATION, "Direct Chat với " + targetPeer.getName() + " đã tồn tại.").showAndWait();
        }
    }

    private void showGroupCreationDialog() {
        ObservableList<Peer> selectedPeers = peerListView.getSelectionModel().getSelectedItems();

        if (selectedPeers.size() < 1) {
            new Alert(AlertType.WARNING, "Vui lòng chọn ít nhất một peer để mời vào nhóm.").showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tạo Nhóm Mới");
        dialog.setHeaderText("Nhập Tên Nhóm");
        dialog.setContentText("Tên Nhóm:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(groupName -> {
            String trimmedName = groupName.trim();
            if (!trimmedName.isEmpty()) {
                List<String> invitedPeerIds = new ArrayList<>();
                for (Peer p : selectedPeers) {
                    invitedPeerIds.add(p.getId());
                }

                // Tạo nhóm và gửi lời mời
                ChatService.createGroupConversation(trimmedName, invitedPeerIds);
                new Alert(AlertType.INFORMATION, "Nhóm '" + trimmedName + "' đã được tạo và lời mời đã được gửi đi!").showAndWait();

            } else {
                new Alert(AlertType.ERROR, "Tên nhóm không được để trống.").showAndWait();
            }
        });
    }

    @Override public void onRemove() { super.onRemove(); }
}