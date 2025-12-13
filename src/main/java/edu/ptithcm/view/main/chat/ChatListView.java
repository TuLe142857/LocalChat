package edu.ptithcm.view.main.chat;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
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
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class ChatListView extends BaseView {
    public ListView<Conversation> conversationListView;
    private ObservableList<Conversation> conversations;
    private final Consumer<Conversation> onConversationSelected;
    private Runnable unsubscribeRunnable;

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

        // Listener này kích hoạt việc hiển thị tin nhắn trong ChatBoxView
        conversationListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onConversationSelected.accept(newVal);
            }
        });

        createGroupButton.setOnAction(e -> {
            new CreateGroupModal((Stage) this.getScene().getWindow(), this::handleNewGroupCreated);
        });

        root.setTop(header);
        root.setCenter(conversationListView);
        this.getChildren().add(root);
    }

    private void handleNewGroupCreated(GroupConversation newGroup) {
        loadData();
        conversationListView.getSelectionModel().select(newGroup);
    }

    @Override
    public void loadData() {
        Platform.runLater(() -> {
            conversations.clear();
            List<Conversation> convList = Cache.getInstance().getConversationList();

            // Sắp xếp theo Lamport Clock (Conversation có tin nhắn mới nhất sẽ lên đầu)
            convList.sort(Comparator.comparing(Conversation::getLamportClock).reversed());

            conversations.addAll(convList);
        });
    }

    @Override public void setupEventBus() {
        // Đăng ký lắng nghe tin nhắn để cập nhật danh sách
        this.unsubscribeRunnable = MessageBus.subscribe(MessageReceivedEvent.class, this::handleMessageReceived);
    }

    // FIX LỖI: Mất chọn (highlight) và không kích hoạt update tin nhắn
    private void handleMessageReceived(MessageReceivedEvent event) {
        final String senderId = event.getMessage().getSenderId();

        Platform.runLater(() -> {
            Conversation currentlySelected = conversationListView.getSelectionModel().getSelectedItem();
            String currentConvId = currentlySelected != null ? currentlySelected.getId() : null;

            // Cờ kiểm tra xem cuộc trò chuyện đang nhận tin nhắn có đang được xem hay không.
            boolean isTargetCurrentlySelected = senderId.equals(currentConvId);

            // 1. Tải lại dữ liệu (loadData()):
            loadData();

            // 2. Tìm Conversation mới sau khi loadData
            Conversation convToSelect = conversations.stream()
                    .filter(c -> c.getId().equals(senderId))
                    .findFirst()
                    .orElse(null);

            if (convToSelect != null) {
                // Nếu Conversation vừa nhận đang được chọn (trước khi reload)
                if (isTargetCurrentlySelected) {
                    // **BƯỚC SỬA LỖI QUAN TRỌNG NHẤT:** // Phải clear selection trước khi select lại cùng một đối tượng (hoặc đối tượng mới cùng ID)
                    // để buộc Listener fired và ListView tô màu lại.
                    conversationListView.getSelectionModel().clearSelection();
                }

                // 3. Re-select. Thao tác này kích hoạt Listener -> ChatLayout -> ChatBoxView.setActiveConversation.
                conversationListView.getSelectionModel().select(convToSelect);
            } else if (currentConvId != null) {
                // Logic phòng ngừa: nếu không tìm thấy convToSelect, cố gắng chọn lại conv cũ nếu có.
                Conversation reSelectConv = conversations.stream()
                        .filter(c -> c.getId().equals(currentConvId))
                        .findFirst()
                        .orElse(null);

                if (reSelectConv != null) {
                    conversationListView.getSelectionModel().select(reSelectConv);
                }
            }
        });
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if(this.unsubscribeRunnable != null) {
            this.unsubscribeRunnable.run();
        }
    }

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
                    GroupConversation gConv = (GroupConversation) conv;
                    int count = gConv.getParticipantList() != null ? gConv.getParticipantList().size() : 0;
                    type = " [GROUP - Thành viên: " + count + "]";
                } else {
                    type = " [Direct Chat]";
                }

                Label typeLabel = new Label(type);
                typeLabel.setStyle("-fx-font-size: 0.8em; -fx-text-fill: #777;");

                VBox box = new VBox(5, nameLabel, typeLabel);
                setGraphic(box);
                setPadding(new Insets(10));

                if (isSelected()) {
                    // Tô màu xanh cho item đang được chọn
                    setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0; -fx-background-color: #bbdefb;");
                } else {
                    setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0; -fx-background-color: #fff;");
                }
            }
        }
    }
}