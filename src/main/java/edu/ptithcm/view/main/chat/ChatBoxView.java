package edu.ptithcm.view.main.chat;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
// Import ChatService để dùng template
import edu.ptithcm.service.ChatService;

public class ChatBoxView extends BaseView {

    private Conversation activeConversation;
    private final Label conversationNameLabel = new Label("Chọn một cuộc trò chuyện");
    private final Button addMemberButton = new Button("➕ Thêm thành viên");
    private final TextArea messageArea = new TextArea();
    private final TextField inputField = new TextField();

    private static final String STYLE_HEADER = "-fx-background-color: #e8eaf6; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;";
    private static final String STYLE_AREA = "-fx-control-inner-background:#fff; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px;";

    @Override protected void init() {}

    @Override
    protected void setupUI() {
        BorderPane layout = new BorderPane();

        // --- 1. Header ---
        HBox headerBox = new HBox(10, conversationNameLabel, addMemberButton);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10));
        headerBox.setStyle(STYLE_HEADER);
        conversationNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.3em; -fx-text-fill: #333;");
        addMemberButton.setStyle("-fx-background-color: #FFC107; -fx-text-fill: #333; -fx-cursor: hand; -fx-background-radius: 5;");
        HBox.setHgrow(conversationNameLabel, javafx.scene.layout.Priority.ALWAYS);

        addMemberButton.setOnAction(e -> handleAddMember());
        addMemberButton.setVisible(false);

        // --- 2. Message Area ---
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setStyle(STYLE_AREA);
        messageArea.setPadding(new Insets(10));

        // --- 3. Input Area ---
        Button sendButton = new Button("Gửi");
        sendButton.setStyle("-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

        inputField.setPromptText("Nhập tin nhắn...");
        HBox.setHgrow(inputField, javafx.scene.layout.Priority.ALWAYS);

        HBox inputBox = new HBox(5, inputField, sendButton);
        inputBox.setPadding(new Insets(10));
        inputBox.setStyle("-fx-background-color: #eee;");

        // Send Action
        sendButton.setOnAction(e -> handleSendMessage());
        inputField.setOnAction(e -> handleSendMessage());

        // --- 4. Layout Assembly ---
        layout.setTop(headerBox);
        layout.setCenter(messageArea);
        layout.setBottom(inputBox);

        this.getChildren().add(layout);
    }

    private void handleSendMessage() {
        if (activeConversation == null) return;
        String content = inputField.getText().trim();
        if (content.isEmpty()) return;

        // [TEMPLATE LOGIC]: Định nghĩa template gọi hàm ChatService (nhưng không gọi)
        // ChatService.sendMessage(activeConversation, content); // Gói tin sẽ được tạo ở đây

        String senderName = Cache.getInstance().getCredential().getName();
        messageArea.appendText(senderName + ": " + content + "\n");
        inputField.clear();
    }

    private void handleAddMember() {
        if (activeConversation instanceof GroupConversation) {
            System.out.println("Mở modal thêm thành viên cho group: " + activeConversation.getName());
            // [TEMPLATE LOGIC]: Định nghĩa template cho việc thêm thành viên
            // GroupService.addMember(activeConversation, newMember);
        }
    }

    public void setActiveConversation(Conversation conversation) {
        if (conversation == null) return;
        this.activeConversation = conversation;
        Platform.runLater(() -> {
            conversationNameLabel.setText(conversation.getName());

            addMemberButton.setVisible(conversation instanceof GroupConversation);

            messageArea.clear();
            messageArea.appendText("--- Đang chat với " + conversation.getName() + " ---\n\n");

            // [MOCK DATA]: Thêm một tin nhắn giả lập để test UI
            messageArea.appendText("Peer giả lập: Hello, đây là tin nhắn mock.\n");
        });
    }

    @Override
    public void loadData() {
        messageArea.setText("Chào mừng đến với LocalChat. Vui lòng chọn một Peer hoặc Group để bắt đầu.");
    }

    @Override public void setupEventBus() {}
}