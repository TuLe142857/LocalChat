package edu.ptithcm.view.main.chat;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.Peer;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.scene.control.SplitPane;

public class ChatLayout extends BaseView {

    private ChatListView chatList;
    private ChatBoxView chatBox;

    public ChatLayout(Object ignored) {}

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
    public void startDirectChat(Peer peer) {
        Conversation conv = Cache.getInstance().getConversation(peer.getId());

        if (conv == null) {
            conv = new DirectConversation(peer);
            Cache.getInstance().addConversation(conv);
            chatList.loadData();
        }

        final Conversation finalConv = conv;
        Platform.runLater(() -> {
            chatList.conversationListView.getSelectionModel().select(finalConv);
            chatBox.setActiveConversation(finalConv);
        });
    }

    @Override public void loadData() {
        chatList.loadData();
        chatBox.loadData();
    }

    @Override public void setupEventBus() {}

    @Override public void onRemove() {
        super.onRemove();
        chatList.onRemove();
        chatBox.onRemove();
    }
}