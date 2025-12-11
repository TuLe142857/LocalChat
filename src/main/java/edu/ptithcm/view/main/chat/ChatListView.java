package edu.ptithcm.view.main.chat;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.model.Peer;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class ChatListView extends BaseView {
    public ListView<Conversation> conversationListView;
    private ObservableList<Conversation> conversations;
    private final Consumer<Conversation> onConversationSelected;

    private static final String STYLE_HEADER = "-fx-background-color: #3f51b5; -fx-border-color: #ccc; -fx-border-width: 0 1 1 0; -fx-text-fill: white;";
    private static final String STYLE_BUTTON = "-fx-background-color: #FFC107; -fx-text-fill: #333; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-cursor: hand; -fx-background-radius: 5;";

    public ChatListView(Consumer<Conversation> onConversationSelected) {
        this.onConversationSelected = onConversationSelected;
    }

    @Override protected void init() {
        conversations = FXCollections.observableArrayList();
    }

    @Override
    protected void setupUI() {
        BorderPane root = new BorderPane();

        HBox header = new HBox(10);
        header.setPadding(new Insets(10));
        header.setStyle(STYLE_HEADER);
        Label title = new Label("Đoạn Chat");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 1.2em; -fx-text-fill: white;");
        Button createGroupButton = new Button("➕ Group");
        createGroupButton.setStyle(STYLE_BUTTON);

        header.getChildren().addAll(title, createGroupButton);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);

        conversationListView = new ListView<>(conversations);
        conversationListView.setCellFactory(lv -> new ConversationListCell());
        conversationListView.setStyle("-fx-background-color: transparent;");

        conversationListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onConversationSelected.accept(newVal);
            }
        });

        createGroupButton.setOnAction(e -> {
            // [TEMPLATE LOGIC]: Giả lập mở modal/kích hoạt logic tạo Group
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Tính năng Tạo Group đã được kích hoạt. (Cần bổ sung logic Backend)", ButtonType.OK);
            alert.showAndWait();
        });

        root.setTop(header);
        root.setCenter(conversationListView);
        this.getChildren().add(root);
    }

    @Override
    public void loadData() {
        conversations.clear();

        // [FIX & MOCK DATA]: Tạo dữ liệu giả lập và thêm trực tiếp vào ObservableList
        // để thay thế cho lời gọi Cache.getConversations() bị thiếu.
        try {
            Peer mockPeerAlice = new Peer("MOCK_ID_1", null, "Peer Alice (Mock)", null, 0);
            Conversation mockDirectChat = new DirectConversation(mockPeerAlice);
            GroupConversation mockGroupChat = new GroupConversation("Nhóm Đồ Án (Mock)");

            conversations.add(mockDirectChat);
            conversations.add(mockGroupChat);

        } catch (Exception e) {
            // Xử lý nếu việc tạo Mock Peer bị lỗi
            System.err.println("Error creating mock data: " + e.getMessage());
        }
    }

    @Override public void setupEventBus() {}

    private class ConversationListCell extends ListCell<Conversation> {
        @Override
        protected void updateItem(Conversation conv, boolean empty) {
            super.updateItem(conv, empty);
            if (empty || conv == null) {
                setText(null);
                setGraphic(null);
            } else {
                Label nameLabel = new Label(conv.getName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em; -fx-text-fill: #333;");

                String type;
                if (conv instanceof GroupConversation) {
                    // [FIX]: Không thể gọi phương thức đếm thành viên. Dùng placeholder.
                    type = " [GROUP - Thành viên: ??]";
                } else {
                    type = " [Direct Chat]";
                }

                Label typeLabel = new Label(type);
                typeLabel.setStyle("-fx-font-size: 0.8em; -fx-text-fill: #777;");

                VBox box = new VBox(5, nameLabel, typeLabel);
                setGraphic(box);
                setPadding(new Insets(10));

                if (isSelected()) {
                    setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0; -fx-background-color: #bbdefb;");
                } else {
                    setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0; -fx-background-color: #fff;");
                }
            }
        }
    }
}