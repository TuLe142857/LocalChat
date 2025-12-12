package edu.ptithcm.view.main.chat;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.Peer;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.scene.control.SplitPane;
import org.tinylog.Logger;

public class ChatLayout extends BaseView {

    private ChatListView chatList;
    private ChatBoxView chatBox;

    public ChatLayout(Object ignored) {
        // Constructor cũ không dùng, giữ lại để không phá vỡ MainLayout.java
    }

    @Override
    protected void init() {
        chatList = new ChatListView(this::handleConversationSelected);
        chatBox = new ChatBoxView();
    }

    @Override
    protected void setupUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(chatList, chatBox);
        splitPane.setDividerPositions(0.3f);
        this.getChildren().add(splitPane);
    }

    // NEW METHOD
    private void handleConversationSelected(Conversation conv) {
        if(conv != null) {
            Platform.runLater(() -> {
                chatBox.setActiveConversation(conv);
            });
        }
    }

    /**
     * Khởi tạo cuộc trò chuyện trực tiếp (Được gọi từ SearchView)
     */
    // NEW METHOD
    public void startDirectChat(Peer peer) {
        // 1. Tìm Conversation đã có
        // ID của DirectConversation là ID của Peer đối tác
        Conversation conv = Cache.getInstance().getConversation(peer.getId());

        if (conv == null) {
            // 2. Tạo Conversation mới nếu chưa có
            Logger.info("Creating new DirectConversation for Peer: " + peer.getName());
            conv = new DirectConversation(peer);
            Cache.getInstance().addConversation(conv);

            // Cần tải lại dữ liệu danh sách chat để hiển thị Conversation mới
            chatList.loadData();
        }

        final Conversation finalConv = conv;
        Platform.runLater(() -> {
            // 3. Chọn Conversation vừa tạo/tìm thấy trong ChatListView
            chatList.conversationListView.getSelectionModel().select(finalConv);

            // 4. Hiển thị nội dung trong ChatBox
            chatBox.setActiveConversation(finalConv);
        });
    }

    @Override public void loadData() {
        chatList.loadData();
        chatBox.loadData();
    }

    @Override public void setupEventBus() {
        chatBox.setupEventBus();
        chatList.setupEventBus();
    }

    @Override public void onRemove() {
        super.onRemove();
        chatList.onRemove();
        chatBox.onRemove();
    }
}