package edu.ptithcm.view.main.chat;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendFailedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
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
    private Button viewMembersButton; // ĐÃ THÊM

    private Runnable unsubscribeReceived;
    private Runnable unsubscribeSendSuccess;
    private Runnable unsubscribeSendFailed;


    @Override
    protected void init() {
        messages = FXCollections.observableArrayList();
        this.setVisible(false);
    }

    @Override
    protected void setupUI() {
        BorderPane layout = new BorderPane();

        // Top Header Area
        conversationNameLabel = new Label("Select a chat");
        conversationNameLabel.setStyle("-fx-font-size: 1.2em; -fx-font-weight: bold;");

        viewMembersButton = new Button("View Members"); // ĐÃ THÊM NÚT
        viewMembersButton.setOnAction(e -> viewGroupMembers());
        viewMembersButton.setManaged(false); // Ẩn/hiện bằng setManaged/setVisible

        HBox headerBox = new HBox(10, conversationNameLabel, viewMembersButton);
        headerBox.setPadding(new Insets(10));
        headerBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(conversationNameLabel, Priority.ALWAYS); // Đẩy nút ViewMembers sang phải

        // Center: Hiển thị tin nhắn
        messageListView = new ListView<>(messages);
        messageListView.setCellFactory(param -> new MessageItem());
        VBox.setVgrow(messageListView, Priority.ALWAYS);
        messageListView.setStyle("-fx-background-color: #f5f5f5;");

        // Bottom: Thanh nhập liệu
        inputField = new TextField();
        inputField.setPromptText("Type message...");
        inputField.setOnAction(e -> sendMessage());
        HBox.setHgrow(inputField, Priority.ALWAYS);

        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        HBox inputBar = new HBox(10, inputField, sendButton);
        inputBar.setPadding(new Insets(10));
        inputBar.setAlignment(Pos.CENTER);

        VBox centerContent = new VBox(headerBox, messageListView, inputBar); // DÙNG headerBox MỚI
        VBox.setVgrow(messageListView, Priority.ALWAYS);

        layout.setCenter(centerContent);

        inputField.setDisable(true);
        sendButton.setDisable(true);

        this.getChildren().add(layout);
    }

    public void setConversation(Conversation conversation) {
        if (currentConversation != conversation) {
            this.currentConversation = conversation;

            // 1. Cập nhật UI
            conversationNameLabel.setText(conversation.getName());

            // 2. Ẩn/hiện nút View Members
            boolean isGroup = conversation instanceof GroupConversation;
            viewMembersButton.setVisible(isGroup);
            viewMembersButton.setManaged(isGroup);

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