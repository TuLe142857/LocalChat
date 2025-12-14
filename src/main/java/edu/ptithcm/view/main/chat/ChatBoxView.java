package edu.ptithcm.view.main.chat;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendFailedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
import edu.ptithcm.bus.event.NewConversationEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.model.Message;
import edu.ptithcm.service.ChatService;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.tinylog.Logger;

import java.util.Optional;
import java.util.function.Consumer;

public class ChatBoxView extends BaseView {

    private Conversation currentConversation;
    private Label conversationNameLabel;
    private ListView<Message> messageListView;
    private ObservableList<Message> messages;
    private TextField inputField;
    private Button sendButton;
    private Button viewMembersButton;
    private Button leaveGroupButton;

    private Runnable unsubscribeReceived;
    private Runnable unsubscribeSendSuccess;
    private Runnable unsubscribeSendFailed;


    @Override
    protected void init() {
        messages = FXCollections.observableArrayList();
        this.setVisible(false);
    }
    protected void setupUI() {
        BorderPane layout = new BorderPane();

        // Background for the entire chat area
        layout.setStyle("-fx-background-color: #ffffff;");

        // Top Header Area
        conversationNameLabel = new Label("Select a chat");
        // Modern Title Style
        conversationNameLabel.setStyle("-fx-font-size: 1.4em; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        viewMembersButton = new Button("View Members");
        viewMembersButton.setOnAction(e -> viewGroupMembers());
        viewMembersButton.setStyle("-fx-background-color: #f0f4f9; -fx-text-fill: #34495e; -fx-background-radius: 5;");

        leaveGroupButton = new Button("Leave Group");
        leaveGroupButton.setOnAction(e -> leaveCurrentGroup());
        leaveGroupButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");


        // VBox để căn phải cho các nút (cần đẩy Label sang trái)
        HBox buttonBox = new HBox(10, viewMembersButton, leaveGroupButton); // Tăng spacing
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        HBox headerBox = new HBox(10, conversationNameLabel, buttonBox);
        headerBox.setPadding(new Insets(15, 20, 15, 20)); // Tăng padding
        headerBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(conversationNameLabel, Priority.ALWAYS);
        HBox.setHgrow(buttonBox, Priority.NEVER);

        // Thêm border dưới nhẹ cho header
        headerBox.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0; -fx-background-color: #ffffff;");

        // Ẩn/hiện nút (Keep original logic)
        viewMembersButton.setManaged(false);
        leaveGroupButton.setManaged(false);

        // Center: Hiển thị tin nhắn
        messageListView = new ListView<>(messages);
        messageListView.setCellFactory(param -> new MessageItem());
        VBox.setVgrow(messageListView, Priority.ALWAYS);
        // Remove background color on list view to show layout's background
        messageListView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // Bottom: Thanh nhập liệu
        inputField = new TextField();
        inputField.setPromptText("Type message...");
        inputField.setOnAction(e -> sendMessage());
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 10 15; -fx-border-color: #dcdde1; -fx-border-width: 1;");

        sendButton = new Button("\u27A4"); // Unicode for Send/Arrow
        sendButton.setOnAction(e -> sendMessage());
        // Modern, circular send button
        sendButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 1.2em; -fx-background-radius: 20; -fx-min-width: 40px; -fx-max-width: 40px; -fx-min-height: 40px; -fx-max-height: 40px; -fx-padding: 0;");


        HBox inputBar = new HBox(10, inputField, sendButton);
        inputBar.setPadding(new Insets(10, 20, 10, 20)); // Tăng padding
        inputBar.setAlignment(Pos.CENTER);
        // Thêm border trên cho input bar
        inputBar.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 1 0 0 0; -fx-background-color: #ffffff;");

        // Combine everything
        VBox centerContent = new VBox(headerBox, messageListView, inputBar);
        VBox.setVgrow(messageListView, Priority.ALWAYS);

        layout.setCenter(centerContent);

        // Initial state (Keep original logic)
        inputField.setDisable(true);
        sendButton.setDisable(true);

        this.getChildren().add(layout);
    }

    public void setConversation(Conversation conversation) {
        if (currentConversation != conversation) {
            this.currentConversation = conversation;

            // 1. Cập nhật UI
            conversationNameLabel.setText(conversation.getName());

            // 2. Ẩn/hiện nút View Members & Leave Group
            boolean isGroup = conversation instanceof GroupConversation;
            viewMembersButton.setVisible(isGroup);
            viewMembersButton.setManaged(isGroup);
            leaveGroupButton.setVisible(isGroup); // Áp dụng cho nút Leave
            leaveGroupButton.setManaged(isGroup);

            // 3. Load tin nhắn
            messages.setAll(conversation.getMessageList());

            // 4. Kích hoạt input
            inputField.setDisable(false);
            sendButton.setDisable(false);

            // 5. Scroll xuống cuối
            Platform.runLater(() -> messageListView.scrollTo(messages.size() - 1));

            // 6. Hiện thị giao diện chat
            this.setVisible(true);
        }
    }

    private void viewGroupMembers() {
        if (currentConversation instanceof GroupConversation) {
            GroupMemberView memberView = new GroupMemberView((GroupConversation) currentConversation);
            memberView.show();
        }
    }

    private void leaveCurrentGroup() {
        if (!(currentConversation instanceof GroupConversation)) {
            return;
        }

        Alert alert = new Alert(
                AlertType.CONFIRMATION,
                "Are you sure you want to leave the group '" + currentConversation.getName() + "'?",
                ButtonType.YES, ButtonType.NO
        );
        alert.setTitle("Confirm Leave Group");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            ChatService.leaveGroup(currentConversation.getId());

            // Sau khi rời nhóm, chúng ta cần chuyển sang trạng thái không chọn cuộc trò chuyện nào
            this.currentConversation = null;
            this.setVisible(false);

            // ĐÃ SỬA: Gửi sự kiện NewConversationEvent để buộc ChatListView tải lại danh sách
            // SỬ DỤNG PHƯƠNG THỨC "emit" CHÍNH XÁC
            MessageBus.emit(new NewConversationEvent(null));

            // Tạm thời, ta chỉ ẩn chatbox đi.
        }
    }

    // ... (Các phương thức khác giữ nguyên)

    private void sendMessage() {
        String content = inputField.getText().trim();
        if (content.isEmpty() || currentConversation == null) {
            return;
        }

        try {
            Message message = currentConversation.createMessage(content);
            ChatService.sendMessage(message);
            inputField.clear();
            messages.setAll(currentConversation.getMessageList());
            Platform.runLater(() -> messageListView.scrollTo(messages.size() - 1));

        } catch (Exception e) {
            Logger.error("Failed to send message: " + e.getMessage());
        }
    }

    @Override
    public void loadData() {}

    @Override
    public void setupEventBus() {
        unsubscribeReceived = MessageBus.subscribe(MessageReceivedEvent.class, this::handleMessageReceived);
        unsubscribeSendSuccess = MessageBus.subscribe(MessageSendSuccessEvent.class, this::handleMessageSendSuccess);
        unsubscribeSendFailed = MessageBus.subscribe(MessageSendFailedEvent.class, this::handleMessageSendFailed);
    }

    private void handleMessageReceived(MessageReceivedEvent event) {
        Message receivedMessage = event.getMessage();

        boolean isDirectChatToMe = receivedMessage.getConversationId().equals(Cache.getInstance().getCredential().getId());
        String expectedConversationId = isDirectChatToMe ? receivedMessage.getSenderId() : receivedMessage.getConversationId();

        if (currentConversation != null && currentConversation.getId().equals(expectedConversationId)) {
            Platform.runLater(() -> {
                messages.setAll(currentConversation.getMessageList());
                messageListView.scrollTo(messages.size() - 1);
            });
        }
    }

    private void updateMessageStatusInUI(String messageId, String conversationId, Message.MessageStatus newStatus) {
        if (currentConversation != null && currentConversation.getId().equals(conversationId)) {
            Platform.runLater(() -> {
                messages.stream()
                        .filter(m -> m.getId().equals(messageId))
                        .findFirst()
                        .ifPresent(message -> {
                            messages.setAll(currentConversation.getMessageList());
                            messageListView.scrollTo(messages.size() - 1);
                        });
            });
        }
    }

    private void handleMessageSendSuccess(MessageSendSuccessEvent event) {
        updateMessageStatusInUI(event.getMessageId(), event.getConversationId(), Message.MessageStatus.SUCCESS);
    }

    private void handleMessageSendFailed(MessageSendFailedEvent event) {
        updateMessageStatusInUI(event.getMessageId(), event.getConversationId(), Message.MessageStatus.FAILED);
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if (unsubscribeReceived != null) unsubscribeReceived.run();
        if (unsubscribeSendSuccess != null) unsubscribeSendSuccess.run();
        if (unsubscribeSendFailed != null) unsubscribeSendFailed.run();
    }
}