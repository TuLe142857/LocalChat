package edu.ptithcm.view.main.search;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.model.Peer;
import edu.ptithcm.service.ChatService;
import edu.ptithcm.view.base.BaseView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Alert.AlertType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SearchView extends BaseView {

    private ListView<Peer> peerListView;
    private ObservableList<Peer> peers;
    private Button startChatButton;
    private Button createGroupButton;
    private Button addMemberButton;

    @Override
    protected void init() {
        peers = FXCollections.observableArrayList();
    }

    @Override
    protected void setupUI() {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #ffffff;"); // White background

        Label title = new Label("Find Peers & Manage Groups");
        title.setStyle("-fx-font-size: 1.8em; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 15 0;");

        // Center: Peer List
        peerListView = new ListView<>(peers);
        peerListView.setCellFactory(param -> new PeerListItem());
        peerListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        peerListView.setStyle("-fx-border-color: #dcdde1; -fx-border-radius: 5;");
        VBox.setVgrow(peerListView, Priority.ALWAYS);

        // Bottom: Action Buttons
        startChatButton = new Button("Start Direct Chat");
        startChatButton.setMaxWidth(Double.MAX_VALUE);
        startChatButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10;");
        startChatButton.setOnAction(e -> startDirectChat());

        createGroupButton = new Button("Create Group Chat");
        createGroupButton.setMaxWidth(Double.MAX_VALUE);
        createGroupButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10;");
        createGroupButton.setOnAction(e -> showGroupCreationDialog());

        addMemberButton = new Button("Add Member to Existing Group");
        addMemberButton.setMaxWidth(Double.MAX_VALUE);
        addMemberButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10;");
        addMemberButton.setOnAction(e -> showAddMemberDialog());

        // Use HBox to make buttons side-by-side if needed, but VBox is also clean
        HBox topButtonBox = new HBox(10, startChatButton, createGroupButton);
        HBox.setHgrow(startChatButton, Priority.ALWAYS);
        HBox.setHgrow(createGroupButton, Priority.ALWAYS);

        VBox buttonBox = new VBox(10, topButtonBox, addMemberButton);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

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
        // Logic refresh Peer List
    }

    private void startDirectChat() {
        // ... (Logic giữ nguyên)
        ObservableList<Peer> selectedPeers = peerListView.getSelectionModel().getSelectedItems();

        if (selectedPeers.size() != 1) {
            new Alert(AlertType.WARNING, "Vui lòng chọn chính xác một peer để bắt đầu trò chuyện trực tiếp.").showAndWait();
            return;
        }

        Peer targetPeer = selectedPeers.get(0);
        Conversation existingConv = Cache.getInstance().getConversation(targetPeer.getId());

        if (existingConv == null) {
            DirectConversation newConv = new DirectConversation(targetPeer);
            Cache.getInstance().addConversation(newConv);
            new Alert(AlertType.INFORMATION, "Đã tạo Direct Chat với " + targetPeer.getName() + ".").showAndWait();
        } else {
            new Alert(AlertType.INFORMATION, "Direct Chat với " + targetPeer.getName() + " đã tồn tại.").showAndWait();
        }
    }

    private void showGroupCreationDialog() {
        // ... (Logic giữ nguyên)
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

                ChatService.createGroupConversation(trimmedName, invitedPeerIds);
                new Alert(AlertType.INFORMATION, "Nhóm '" + trimmedName + "' đã được tạo và lời mời đã được gửi đi!").showAndWait();

            } else {
                new Alert(AlertType.ERROR, "Tên nhóm không được để trống.").showAndWait();
            }
        });
    }

    private void showAddMemberDialog() {
        ObservableList<Peer> selectedPeers = peerListView.getSelectionModel().getSelectedItems();

        if (selectedPeers.isEmpty()) {
            new Alert(AlertType.WARNING, "Vui lòng chọn ít nhất một peer để thêm vào nhóm.").showAndWait();
            return;
        }

        // 1. Lấy danh sách các Group mà tôi là thành viên để thêm người
        List<GroupConversation> myGroups = Cache.getInstance().getConversationList().stream()
                .filter(c -> c instanceof GroupConversation)
                .map(c -> (GroupConversation)c)
                .collect(Collectors.toList());

        if (myGroups.isEmpty()) {
            new Alert(AlertType.ERROR, "Bạn không có nhóm nào để thêm thành viên.").showAndWait();
            return;
        }

        // 2. Tạo Dialog để chọn nhóm
        ChoiceDialog<GroupConversation> dialog = new ChoiceDialog<>(myGroups.get(0), myGroups);
        dialog.setTitle("Add Members");
        dialog.setHeaderText("Chọn nhóm để thêm thành viên");
        dialog.setContentText("Chọn Nhóm:");

        Optional<GroupConversation> result = dialog.showAndWait();

        result.ifPresent(selectedGroup -> {
            List<String> invitedIds = new ArrayList<>();
            String peersAdded = "";

            for (Peer p : selectedPeers) {
                // Kiểm tra nếu Peer đã là thành viên của nhóm, không gửi lời mời
                if (selectedGroup.getParticipant(p.getId()) == null) {
                    invitedIds.add(p.getId());
                    peersAdded += p.getName() + ", ";
                }
            }

            if (invitedIds.isEmpty()) {
                new Alert(AlertType.INFORMATION, "Tất cả các peer đã chọn đã là thành viên của nhóm này.").showAndWait();
                return;
            }

            // 3. Gửi lời mời cho từng Peer
            for (String peerId : invitedIds) {
                ChatService.invitePeerToGroup(selectedGroup.getId(), peerId);
            }

            new Alert(AlertType.INFORMATION,
                    String.format("Đã gửi lời mời tới %d thành viên mới (%s) cho nhóm '%s'.",
                            invitedIds.size(),
                            peersAdded.substring(0, peersAdded.length() - 2),
                            selectedGroup.getName())).showAndWait();
        });
    }

    @Override public void onRemove() { super.onRemove(); }
}