package edu.ptithcm.view.main.chat;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendFailedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.Message;
import edu.ptithcm.service.ChatService;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.tinylog.Logger;

import java.util.function.Consumer;

public class ChatBoxView extends BaseView {

    private Conversation currentConversation;
    private Label conversationNameLabel;
    private ListView<Message> messageListView;
    private ObservableList<Message> messages;
    private TextField inputField;
    private Button sendButton;
    private Runnable unsubscribeReceived;
    private Runnable unsubscribeSendSuccess;
    private Runnable unsubscribeSendFailed;


    @Override
    protected void init() {
        messages = FXCollections.observableArrayList();
        // Ban đầu ẩn đi
        this.setVisible(false);
    }

    @Override
    protected void setupUI() {
        BorderPane layout = new BorderPane();

        // Top: Tên cuộc trò chuyện
        conversationNameLabel = new Label("Select a chat");
        conversationNameLabel.setStyle("-fx-font-size: 1.2em; -fx-padding: 10; -fx-border-width: 0 0 1 0; -fx-border-color: #ccc;");
        conversationNameLabel.setMaxWidth(Double.MAX_VALUE);

        // Center: Hiển thị tin nhắn
        messageListView = new ListView<>(messages);
        messageListView.setCellFactory(param -> new MessageItem());
        VBox.setVgrow(messageListView, Priority.ALWAYS);
        messageListView.setStyle("-fx-background-color: #f5f5f5;"); // Màu nền chat

        // Bottom: Thanh nhập liệu
        inputField = new TextField();
        inputField.setPromptText("Type message...");
        inputField.setOnAction(e -> sendMessage()); // Send on Enter
        HBox.setHgrow(inputField, Priority.ALWAYS);

        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        HBox inputBar = new HBox(10, inputField, sendButton);
        inputBar.setPadding(new Insets(10));
        inputBar.setAlignment(Pos.CENTER);

        VBox centerContent = new VBox(conversationNameLabel, messageListView, inputBar);
        VBox.setVgrow(messageListView, Priority.ALWAYS);

        layout.setCenter(centerContent);

        // Kích hoạt/Vô hiệu hóa input khi chưa chọn chat
        inputField.setDisable(true);
        sendButton.setDisable(true);

        this.getChildren().add(layout);
    }

    public void setConversation(Conversation conversation) {
        if (currentConversation != conversation) {
            this.currentConversation = conversation;

            // 1. Cập nhật UI
            conversationNameLabel.setText(conversation.getName());

            // 2. Load tin nhắn
            messages.setAll(conversation.getMessageList());

            // 3. Kích hoạt input
            inputField.setDisable(false);
            sendButton.setDisable(false);

            // 4. Scroll xuống cuối
            Platform.runLater(() -> messageListView.scrollTo(messages.size() - 1));

            // 5. Hiện thị giao diện chat
            this.setVisible(true);
        }
    }

    private void sendMessage() {
        String content = inputField.getText().trim();
        if (content.isEmpty() || currentConversation == null) {
            return;
        }

        try {
            // 1. Tạo tin nhắn, tự tăng Lamport Clock và thêm vào Conversation.messages
            Message message = currentConversation.createMessage(content);

            // 2. Gửi đi
            ChatService.sendMessage(message);

            // 3. Cập nhật UI
            inputField.clear();
            messages.setAll(currentConversation.getMessageList()); // Reload list for correct sorting/status
            Platform.runLater(() -> messageListView.scrollTo(messages.size() - 1));

        } catch (Exception e) {
            Logger.error("Failed to send message: " + e.getMessage());
        }
    }

    @Override
    public void loadData() {}

    @Override
    public void setupEventBus() {
        // Subscribe to incoming messages
        unsubscribeReceived = MessageBus.subscribe(MessageReceivedEvent.class, this::handleMessageReceived);

        // Subscribe to outgoing message status updates
        unsubscribeSendSuccess = MessageBus.subscribe(MessageSendSuccessEvent.class, this::handleMessageSendSuccess);
        unsubscribeSendFailed = MessageBus.subscribe(MessageSendFailedEvent.class, this::handleMessageSendFailed);
    }

    private void handleMessageReceived(MessageReceivedEvent event) {
        Message receivedMessage = event.getMessage();

        // Xác định ID của cuộc trò chuyện dựa trên loại tin nhắn
        boolean isDirectChatToMe = receivedMessage.getConversationId().equals(Cache.getInstance().getCredential().getId());
        String expectedConversationId = isDirectChatToMe ? receivedMessage.getSenderId() : receivedMessage.getConversationId();

        if (currentConversation != null && currentConversation.getId().equals(expectedConversationId)) {
            Platform.runLater(() -> {
                // Tin nhắn đã được thêm vào conversation trong ChatService.onReceiveMessage
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
                            // Cập nhật trạng thái trực tiếp trên đối tượng Message trong ObservableList
                            // Tuy nhiên, để force ListView cập nhật cell, ta cần set lại list
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