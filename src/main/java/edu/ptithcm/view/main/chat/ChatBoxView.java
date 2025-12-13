package edu.ptithcm.view.main.chat;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
import edu.ptithcm.bus.event.MessageSendingEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.model.Message;
import edu.ptithcm.model.Peer;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.tinylog.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatBoxView extends BaseView {

    private Conversation activeConversation;
    private Label conversationNameLabel;
    private Button addMemberButton;
    private ScrollPane messageScrollPane;
    private VBox messageContainer;
    private TextField inputField;
    private Runnable unsubscribeReceiver;
    private Runnable unsubscribeSuccess;


    private static final String STYLE_HEADER = "-fx-background-color: #e8eaf6; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;";
    private static final String STYLE_AREA = "-fx-control-inner-background:#fff; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-background-color: #f5f5f5;";

    // NEW STYLES for Bubble Chat
    private static final String STYLE_BUBBLE_ME =
            "-fx-background-color: #dcf8c6; " +
                    "-fx-padding: 8px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-text-fill: #333;";

    private static final String STYLE_BUBBLE_OTHER =
            "-fx-background-color: #ffffff; " +
                    "-fx-padding: 8px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-border-color: #ccc; " +
                    "-fx-border-width: 1px; " +
                    "-fx-text-fill: #333;";


    @Override
    protected void init() {
        conversationNameLabel = new Label("Chọn một cuộc trò chuyện");
        addMemberButton = new Button("➕ Thêm thành viên");
        inputField = new TextField();

        // NEW UI COMPONENTS: Message Area
        messageContainer = new VBox(5); // 5px spacing between messages
        messageContainer.setPadding(new Insets(10));
        messageContainer.setFillWidth(true); // Quan trọng để HBox bên trong có thể căn lề

        messageScrollPane = new ScrollPane(messageContainer);
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setStyle(STYLE_AREA);
        messageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

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

        // --- 2. Message Area (Đã thay bằng ScrollPane) ---
        layout.setCenter(messageScrollPane);

        // Auto-scroll to bottom when content changes (Cần thêm Listener)
        messageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            messageScrollPane.setVvalue(1.0);
        });

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
        layout.setBottom(inputBox);

        this.getChildren().add(layout);
    }

    private void handleSendMessage() {
        if (activeConversation == null) return;
        String content = inputField.getText().trim();
        if (content.isEmpty()) return;

        // 1. Tạo Message và thêm vào Conversation (cập nhật Lamport Clock)
        Message message = activeConversation.createMessage(content);

        // 2. Cập nhật UI ngay lập lập
        inputField.clear();
        updateMessageArea();

        // 3. Gửi sự kiện qua MessageBus
        MessageBus.emit(new MessageSendingEvent(message));
    }

    private void handleAddMember() {
        if (activeConversation instanceof GroupConversation) {
            Logger.info("Mở modal thêm thành viên cho group: " + activeConversation.getName());
        }
    }

    // NEW LOGIC: Rework updateMessageArea để tạo Bubble Chat
    private void updateMessageArea() {
        Logger.debug("Update message area");
        if (activeConversation == null) {
            Logger.debug("Bi Null roi ne===================================");
            return;
        }

        Platform.runLater(() -> {
            messageContainer.getChildren().clear();
            String myId = Cache.getInstance().getCredential().getId();

            for (Message m : activeConversation.getMessageList()) {
                boolean isMe = m.getSenderId().equals(myId);
                String statusMarker = getStatusMarker(m.getStatus());
                Logger.debug(m.getStatus());

                // 1. Tạo Label chứa nội dung tin nhắn
                Label contentLabel = new Label(m.getContent());
                contentLabel.setWrapText(true);
                contentLabel.setMaxWidth(400);

                // 2. Tạo Label chứa trạng thái/tên người gửi
                Label statusInfoLabel = new Label();
                statusInfoLabel.setStyle("-fx-font-size: 0.8em; -fx-text-fill: #888;");

                if (isMe) {
                    // Tin nhắn của tôi: [Trạng thái]
                    statusInfoLabel.setText(statusMarker); // Chỉ hiển thị trạng thái
                    contentLabel.setStyle(STYLE_BUBBLE_ME);
                } else {
                    // Tin nhắn của người khác: [Tên người gửi]
                    String senderName = getSenderName(m.getSenderId(), activeConversation);
                    statusInfoLabel.setText(senderName);
                    contentLabel.setStyle(STYLE_BUBBLE_OTHER);
                }

                // 3. Xây dựng Bong bóng chat (Bubble)
                VBox bubble = new VBox(2); // VBox chứa trạng thái/tên và nội dung

                if(isMe){
                    // Tin nhắn của tôi: [Nội dung] và [Trạng thái] nằm dưới, căn phải
                    bubble.setAlignment(Pos.CENTER_RIGHT);
                    bubble.getChildren().addAll(contentLabel, statusInfoLabel);
                } else {
                    // Tin nhắn của người khác: [Tên] và [Nội dung], căn trái
                    bubble.setAlignment(Pos.CENTER_LEFT);
                    bubble.getChildren().addAll(statusInfoLabel, contentLabel);
                }

                bubble.setPadding(new Insets(0, 5, 0, 5)); // Spacing quanh bubble

                // 4. Căn lề Bubble bằng HBox
                HBox messageWrapper = new HBox();
                messageWrapper.getChildren().add(bubble);

                // Căn lề phải cho tin nhắn của mình, trái cho tin nhắn của người khác
                messageWrapper.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                // 5. Thêm vào Container
                messageContainer.getChildren().add(messageWrapper);
            }
        });
    }

    private String getSenderName(String senderId, Conversation conversation) {
        if (conversation instanceof DirectConversation) {
            return ((DirectConversation) conversation).getPartner().getName();
        } else if (conversation instanceof GroupConversation) {
            Peer peer = Cache.getInstance().getPeer(senderId);
            return peer != null ? peer.getName() : "Peer #" + senderId.substring(0, 4);
        }
        return "Unknown Peer";
    }

    private String getStatusMarker(Message.MessageStatus status) {
        switch (status) {
            case PENDING: return "⏳";
            case SUCCESS: return "✔️";
            case FAILED: return "❌";
            default: return "";
        }
    }

    public void setActiveConversation(Conversation conversation) {
        if (conversation == null) return;
        this.activeConversation = conversation;
        Platform.runLater(() -> {
            conversationNameLabel.setText(conversation.getName());
            addMemberButton.setVisible(conversation instanceof GroupConversation);
            updateMessageArea(); // Load lịch sử tin nhắn
        });
    }

    @Override
    public void loadData() {
        setActiveConversation(null);
        // messageArea.setText("..."); (Bây giờ đã được xử lý bằng cách clear messageContainer)
    }

    @Override
    public void setupEventBus() {
        if(unsubscribeReceiver != null) unsubscribeReceiver.run();
        if(unsubscribeSuccess != null) unsubscribeSuccess.run();

        unsubscribeReceiver = MessageBus.subscribe(MessageReceivedEvent.class, this::handleMessageReceived);
        unsubscribeSuccess = MessageBus.subscribe(MessageSendSuccessEvent.class, this::handleMessageSuccess);
    }

    // Logic này vẫn đảm bảo cập nhật tin nhắn đến (tự động)
    private void handleMessageReceived(MessageReceivedEvent event) {
        Platform.runLater(() -> {
            if (activeConversation == null) return;
            String senderId = event.getMessage().getSenderId();
            if (activeConversation instanceof DirectConversation && activeConversation.getId().equals(senderId)) {
                updateMessageArea();
            }
        });
    }

    // Logic này cập nhật icon trạng thái tin nhắn gửi đi (SUCCESS)
    private void handleMessageSuccess(MessageSendSuccessEvent event) {
        // Chỉ cập nhật UI nếu tin nhắn thuộc Conversation đang mở
        Logger.info("SUCCESS ROI NE============================");
        if (activeConversation != null && activeConversation.getId().equals(event.getConversationId())) {
            Logger.info("Ko thoa dieu kien roi ma oi");
            Platform.runLater(this::updateMessageArea);
        }
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if(unsubscribeReceiver != null) unsubscribeReceiver.run();
        if(unsubscribeSuccess != null) unsubscribeSuccess.run();
    }
}