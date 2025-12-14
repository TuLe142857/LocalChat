package edu.ptithcm.view.main.chat;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.GroupConversation;
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

    // Default constructor cho tính tương thích
    public ChatListView() {
        this.onConversationSelected = c -> {};
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
            if (newVal != null && onConversationSelected != null) {
                onConversationSelected.accept(newVal);
            }
        });

        createGroupButton.setOnAction(e -> {
            // Lấy Stage của cửa sổ hiện tại để làm owner cho modal
            Stage ownerStage = (Stage) this.getScene().getWindow();
            new CreateGroupModal(ownerStage, this::handleNewGroupCreated);
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
        conversations.clear();
        List<Conversation> convList = Cache.getInstance().getConversationList();

        // Sắp xếp theo Lamport Clock (Conversation có tin nhắn mới nhất sẽ lên đầu)
        convList.sort(Comparator.comparing(Conversation::getLamportClock).reversed());

        conversations.addAll(convList);
    }

    @Override public void setupEventBus() {
        // Đăng ký lắng nghe tin nhắn để cập nhật danh sách
        if (this.unsubscribeRunnable == null) {
            this.unsubscribeRunnable = MessageBus.subscribe(MessageReceivedEvent.class, this::handleMessageReceived);
        }
    }

    private void handleMessageReceived(MessageReceivedEvent event) {
        final String senderId = event.getMessage().getSenderId();
        final String convIdFromMessage = event.getMessage().getConversationId();
        final String myId = Cache.getInstance().getCredential().getId();

        // Xác định ID Conversation cục bộ (là senderId nếu là Direct chat đến mình, ngược lại là group ID)
        final String localConversationId = convIdFromMessage.equals(myId) ? senderId : convIdFromMessage;

        Platform.runLater(() -> {
            Conversation currentlySelected = conversationListView.getSelectionModel().getSelectedItem();
            String currentConvId = currentlySelected != null ? currentlySelected.getId() : null;

            // 1. Tải lại dữ liệu (loadData()):
            loadData();

            // 2. Nếu Conversation vừa nhận tin nhắn là Conversation đang được chọn,
            //    chúng ta cần re-select để kích hoạt listener trong ChatLayout -> ChatBoxView
            if (localConversationId.equals(currentConvId)) {
                Conversation convToSelect = conversations.stream()
                        .filter(c -> c.getId().equals(localConversationId))
                        .findFirst()
                        .orElse(null);

                if (convToSelect != null) {
                    // Phải clear selection trước khi select lại cùng một đối tượng
                    conversationListView.getSelectionModel().clearSelection();

                    // Re-select. Thao tác này kích hoạt Listener
                    conversationListView.getSelectionModel().select(convToSelect);
                }
            } else if (currentConvId != null) {
                // Logic phòng ngừa: nếu tin nhắn không thuộc conv đang mở, nhưng conv đang mở
                // đã bị mất highlight do reload data, hãy cố gắng chọn lại conv cũ.
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
            this.unsubscribeRunnable = null;
        }
    }

    private class ConversationListCell extends ListCell<Conversation> {
        @Override
        protected void updateItem(Conversation conv, boolean empty) {
            super.updateItem(conv, empty);
            if (empty || conv == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
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