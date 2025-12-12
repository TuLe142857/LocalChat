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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.tinylog.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ChatBoxView extends BaseView {

    private Conversation activeConversation;
    private Label conversationNameLabel;
    private Button addMemberButton;
    private TextArea messageArea;
    private TextField inputField;
    private Runnable unsubscribeReceiver;
    private Runnable unsubscribeSuccess;
    private ScheduledExecutorService executor;

    private static final String STYLE_HEADER = "-fx-background-color: #e8eaf6; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;";
    private static final String STYLE_AREA = "-fx-control-inner-background:#fff; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px;";

    @Override
    protected void init() {
        conversationNameLabel = new Label("Chọn một cuộc trò chuyện");
        addMemberButton = new Button("➕ Thêm thành viên");
        messageArea = new TextArea();
        inputField = new TextField();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
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

        // 1. Tạo Message và thêm vào Conversation (cập nhật Lamport Clock)
        Message message = activeConversation.createMessage(content);

        // 2. Cập nhật UI ngay lập tức
        inputField.clear();
        updateMessageArea();

        // 3. Gửi sự kiện qua MessageBus
        MessageBus.emit(new MessageSendingEvent(message));
    }

    private void handleAddMember() {
        if (activeConversation instanceof GroupConversation) {
            // [TODO]: Logic mở modal thêm thành viên cho Group
            Logger.info("Mở modal thêm thành viên cho group: " + activeConversation.getName());
        }
    }

    private void updateMessageArea() {
        if (activeConversation == null) return;

        Platform.runLater(() -> {
            messageArea.clear();
            String myId = Cache.getInstance().getCredential().getId();

            for (Message m : activeConversation.getMessageList()) {
                String senderName = m.getSenderId().equals(myId)
                        ? Cache.getInstance().getCredential().getName()
                        : getSenderName(m.getSenderId(), activeConversation);

                String statusMarker = getStatusMarker(m.getStatus());

                messageArea.appendText(String.format("%s: %s %s\n", senderName, m.getContent(), statusMarker));
            }
        });
    }

    private String getSenderName(String senderId, Conversation conversation) {
        if (conversation instanceof DirectConversation) {
            return ((DirectConversation) conversation).getPartner().getName();
        } else if (conversation instanceof GroupConversation) {
            // [TODO]: Cần có cách tra cứu tên Peer trong Group Conversation
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
            updateMessageArea();
        });
    }

    @Override
    public void loadData() {
        // [FIX]: Set Active Conversation là null khi loadData để reset
        setActiveConversation(null);
        messageArea.setText("Chào mừng đến với LocalChat. Vui lòng chọn một Peer hoặc Group để bắt đầu.");
    }

    @Override
    public void setupEventBus() {
        // Hủy đăng ký cũ nếu có
        if(unsubscribeReceiver != null) unsubscribeReceiver.run();
        if(unsubscribeSuccess != null) unsubscribeSuccess.run();

        // Lắng nghe MessageReceivedEvent (khi có tin nhắn đến)
        unsubscribeReceiver = MessageBus.subscribe(MessageReceivedEvent.class, this::handleMessageReceived);

        // Lắng nghe MessageSendSuccessEvent (khi tin nhắn đi đã được xác nhận)
        unsubscribeSuccess = MessageBus.subscribe(MessageSendSuccessEvent.class, this::handleMessageSuccess);
    }

    private void handleMessageReceived(MessageReceivedEvent event) {
        // Chỉ cập nhật UI nếu tin nhắn thuộc Conversation đang mở
        String receivedConvId = event.getMessage().getConversationId();

        // Logic tìm conversation:
        // Nếu là Direct Chat, ConversationId là Id của Peer đối tác, còn tin nhắn đến
        // có ConversationId = Id của mình.
        // ChatService đã xử lý việc tìm/tạo Conversation đúng.
        // Ta chỉ cần lấy ConversationId từ ChatService.onReceiveMessage.

        // Vì MessageBus đã gọi ChatService.onReceiveMessage trước, nên tin nhắn đã được thêm vào Conversation
        // Ta chỉ cần kiểm tra xem tin nhắn có thuộc Conversation đang hiển thị không
        if (activeConversation != null && activeConversation.getId().equals(receivedConvId)) {
            // Cập nhật UI trên JavaFX Thread
            Platform.runLater(this::updateMessageArea);
        } else {
            // Nếu là Direct Chat mới được tạo, conversationId sẽ là SenderId
            String senderId = event.getMessage().getSenderId();
            if(activeConversation != null && activeConversation.getId().equals(senderId)){
                Platform.runLater(this::updateMessageArea);
            }
        }
    }

    private void handleMessageSuccess(MessageSendSuccessEvent event) {
        // Chỉ cập nhật UI nếu tin nhắn thuộc Conversation đang mở
        if (activeConversation != null && activeConversation.getId().equals(event.getConversationId())) {
            // Dùng Platform.runLater để đảm bảo cập nhật UI Thread
            Platform.runLater(this::updateMessageArea);
        }
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if(unsubscribeReceiver != null) unsubscribeReceiver.run();
        if(unsubscribeSuccess != null) unsubscribeSuccess.run();
        if(executor != null) executor.shutdownNow();
    }
}