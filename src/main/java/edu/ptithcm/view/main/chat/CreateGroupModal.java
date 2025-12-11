package edu.ptithcm.view.main.chat;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.model.Peer;
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

import java.util.List;
import java.util.function.Consumer;

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

        // [MOCK DATA]: Giả lập Peer cho modal chọn thành viên
        try {
            availablePeers.add(new Peer("MOCK_ID_1", null, "Peer Alice", null, 0));
            availablePeers.add(new Peer("MOCK_ID_2", null, "Peer Bob", null, 0));
        } catch (Exception e) {}

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
                setText(empty ? null : peer.getName() + " (Mock IP)");
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

        // --- Show ---
        Scene scene = new Scene(this, 400, 500);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private void createGroup() {
        String groupName = groupNameField.getText().trim();
        List<Peer> selectedPeers = peerListView.getSelectionModel().getSelectedItems();

        if (groupName.isEmpty() || selectedPeers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng nhập tên Group và chọn ít nhất một thành viên.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // [MOCK DATA]: Tạo Group Conversation
        GroupConversation newGroup = new GroupConversation(groupName);

        // [MOCK LOGIC]: Giả lập thêm thành viên
        newGroup.addParticipants(Cache.getInstance().getMyPeer());
        for (Peer peer : selectedPeers) {
            newGroup.addParticipants(peer);
        }

        // [TEMPLATE]: Logic thông báo cho Backend (GroupService.createGroup(newGroup))

        // Giả lập thêm vào cache và gọi callback
        Cache.getInstance().addConversation(newGroup);
        onCreate.accept(newGroup);
        dialogStage.close();
    }
}