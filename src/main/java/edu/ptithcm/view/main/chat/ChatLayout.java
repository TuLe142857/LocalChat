package edu.ptithcm.view.main.chat;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ChatLayout extends BaseView {

    private ChatListView chatList;
    private ChatBoxView chatBox;

    // CLIENT-SIDE STATE FOR UNREAD COUNT (Key: Conversation ID, Value: Unread Count)
    // Dùng để đếm số tin nhắn mới khi user đang focus vào chat khác.
    private final ConcurrentHashMap<String, Integer> unreadCounts = new ConcurrentHashMap<>();

    private Runnable unsubscribeMessageReceived;

    @Override
    protected void init() {
        // Lắng nghe sự kiện tin nhắn đến
        this.unsubscribeMessageReceived = MessageBus.subscribe(MessageReceivedEvent.class, this::handleMessageReceived);
    }

    // Xử lý khi nhận tin nhắn mới
    private void handleMessageReceived(MessageReceivedEvent event) {
        String conversationId;
        // 1. Xác định ID Conversation thực tế trên client cache
        if (event.getMessage().getConversationId().equals(Cache.getInstance().getCredential().getId())) {
            // Direct Message: ID Conversation là ID của người gửi
            conversationId = event.getMessage().getSenderId();
        } else {
            // Group Chat: ID Conversation là ID của Group
            conversationId = event.getMessage().getConversationId();
        }

        // 2. CHỈ cập nhật count nếu cuộc trò chuyện KHÔNG phải là cuộc trò chuyện đang được mở
        if (chatBox.getCurrentConversation() == null || !chatBox.getCurrentConversation().getId().equals(conversationId)) {
            // Tăng số lượng chưa đọc lên 1
            unreadCounts.compute(conversationId, (k, v) -> v == null ? 1 : v + 1);

            // Yêu cầu ChatList (UI) tự refresh để hiển thị dấu đỏ
            Platform.runLater(chatList::refreshListUI);
        } else {
            // Nếu là chat hiện tại, không tăng count, chỉ refresh ChatList
            // Vẫn cần gọi refreshListUI để ChatListItem có thể cập nhật trạng thái Pending/Failed nếu cần
            Platform.runLater(chatList::refreshListUI);
        }
    }


    @Override
    protected void setupUI() {
        // Function để ChatListItem gọi lấy số đếm
        Function<String, Integer> getUnreadCount = (convId) -> unreadCounts.getOrDefault(convId, 0);

        // Function để ChatListView gọi reset số đếm khi item được click
        // RefreshCallback được truyền vào để ChatListView tự cập nhật UI sau khi reset state
        BiConsumer<String, Runnable> resetUnreadCount = (convId, refreshCallback) -> {
            if (unreadCounts.containsKey(convId) && unreadCounts.get(convId) > 0) {
                unreadCounts.put(convId, 0);
                // Trigger refresh trên ChatListView để xóa dấu đỏ
                Platform.runLater(refreshCallback);
            }
        };

        chatBox = new ChatBoxView();

        // Truyền các callbacks quản lý state cho ChatListView
        chatList = new ChatListView(chatBox::setConversation, resetUnreadCount, getUnreadCount);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(chatList, chatBox);
        splitPane.setDividerPositions(0.3f); // 30% cho list

        VBox.setVgrow(splitPane, Priority.ALWAYS);

        this.getChildren().add(splitPane);
    }

    @Override public void loadData() {}
    @Override public void setupEventBus() {}

    @Override
    public void onRemove() {
        super.onRemove();
        if (unsubscribeMessageReceived != null) {
            unsubscribeMessageReceived.run();
        }
        // Khi remove Layout cha, phải đảm bảo các view con cũng được dọn dẹp
        if (chatList != null) {
            chatList.onRemove();
        }
        if (chatBox != null) {
            chatBox.onRemove();
        }
    }
}