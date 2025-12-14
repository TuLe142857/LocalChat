package edu.ptithcm.view.main.chat;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.model.Peer;
import edu.ptithcm.service.ChatService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.tinylog.Logger;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// Modal dùng để tạo Group và chọn thành viên ban đầu
public class CreateGroupModal extends VBox {
    private final Stage dialogStage;
    private final Consumer<GroupConversation> onCreate;
    private final TextField groupNameField;
    private final ListView<Peer> peerListView;
    private final ObservableList<Peer> availablePeers;

    public CreateGroupModal(Stage ownerStage, Consumer<GroupConversation> onCreate) {
        this.onCreate = onCreate;

        dialogStage = new Stage();
        dialogStage.initOwner(ownerStage);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle("Tạo Group Mới");

        availablePeers = FXCollections.observableArrayList();

        // --- UI Components ---
        Label title = new Label("Chọn thành viên và đặt tên Group");
        title.setStyle("-fx-font-size: 1.3em; -fx-font-weight: bold; -fx-text-fill: #3f51b5;");

        groupNameField = new TextField();
        groupNameField.setPromptText("Tên Group...");

        peerListView = new ListView<>(availablePeers);
        peerListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        peerListView.setCellFactory(lv -> new ListCell<Peer>() {
            @Override
            protected void updateItem(Peer peer, boolean empty) {
                super.updateItem(peer, empty);
                setText(empty ? null : peer.getName() + " (" + (peer != null && peer.getIp() != null ? peer.getIp().getHostAddress() : "Unknown IP") + ")");
            }
        });

        Button createButton = new Button("Tạo Group");
        createButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        createButton.setOnAction(e -> createGroup());

        Button cancelButton = new Button("Hủy");
        cancelButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 5;");
        cancelButton.setOnAction(e -> dialogStage.close());

        // --- Layout ---
        this.setPadding(new Insets(20));
        this.setSpacing(15);
        this.setStyle("-fx-background-color: #fff;");
        this.getChildren().addAll(
                title,
                new Label("Tên Group:"),
                groupNameField,
                new Label("Chọn thành viên:"),
                peerListView,
                new HBox(10, createButton, cancelButton)
        );
        ((HBox) this.getChildren().get(this.getChildren().size() - 1)).setAlignment(Pos.CENTER_RIGHT);

        // Load data for peer list
        loadData();

        // --- Show ---
        Scene scene = new Scene(this, 400, 500);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private void loadData() {
        // Lấy danh sách Peer đã biết (trừ bản thân)
        List<Peer> peers = Cache.getInstance().getPeerList();
        String myId = Cache.getInstance().getCredential() != null ? Cache.getInstance().getCredential().getId() : null;
        if(myId != null) peers.removeIf(peer -> peer.getId().equals(myId));
        availablePeers.setAll(peers);
    }

    private void createGroup() {
        String groupName = groupNameField.getText().trim();
        List<Peer> selectedPeers = peerListView.getSelectionModel().getSelectedItems();

        if (groupName.isEmpty() || selectedPeers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng nhập tên Group và chọn ít nhất một thành viên.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        List<String> invitedPeerIds = selectedPeers.stream()
                .map(Peer::getId)
                .collect(Collectors.toList());

        // Gọi ChatService để tạo group và gửi lời mời
        GroupConversation newGroup = ChatService.createGroupConversation(groupName, invitedPeerIds);

        Logger.info("Created new group: " + newGroup.getName() + " (" + newGroup.getId() + ")");

        // Gọi callback để cập nhật ChatListView và chọn group mới
        onCreate.accept(newGroup);
        dialogStage.close();
    }
}