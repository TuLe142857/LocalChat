package edu.ptithcm.view.main.chat;

import edu.ptithcm.bus.MessageBus;
import edu.ptithcm.bus.event.MessageReceivedEvent;
import edu.ptithcm.bus.event.MessageSendSuccessEvent;
import edu.ptithcm.bus.event.MessageSendFailedEvent;
import edu.ptithcm.bus.event.NewConversationEvent;
import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Conversation;
import edu.ptithcm.view.base.BaseView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChatListView extends BaseView {
    private ListView<Conversation> conversationListView;
    private ObservableList<Conversation> conversations;
    private Runnable unsubscribeNewConversation;

    private List<Runnable> messageUpdatesUnsubscribers;

    private final Consumer<Conversation> onConversationSelected;

    // YÊU CẦU MỚI: Callbacks để quản lý Unread Count từ ChatLayout
    private final BiConsumer<String, Runnable> resetUnreadCountCallback;
    private final Function<String, Integer> getUnreadCountCallback;


    public ChatListView(
            Consumer<Conversation> onConversationSelected,
            BiConsumer<String, Runnable> resetUnreadCountCallback,
            Function<String, Integer> getUnreadCountCallback) {
        this.onConversationSelected = onConversationSelected;
        this.resetUnreadCountCallback = resetUnreadCountCallback;
        this.getUnreadCountCallback = getUnreadCountCallback;
    }

    @Override
    protected void init() {
        conversations = FXCollections.observableArrayList();
        messageUpdatesUnsubscribers = new ArrayList<>();
    }

    @Override
    protected void setupUI() {
        conversationListView = new ListView<>(conversations);

        // Truyền callback vào Cell Factory
        conversationListView.setCellFactory(param -> new ChatListItem(getUnreadCountCallback));

        // Handle selection event: Reset unread count when an item is clicked
        conversationListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        onConversationSelected.accept(newVal);

                        // YÊU CẦU MỚI: Reset unread count cho cuộc trò chuyện đã chọn
                        // Gọi callback để ChatLayout xử lý reset state
                        resetUnreadCountCallback.accept(newVal.getId(), this::refreshListUI);
                    }
                });

        VBox.setVgrow(conversationListView, Priority.ALWAYS);

        this.getChildren().add(conversationListView);
    }

    @Override
    public void loadData() {
        // Lấy Conversation đang được chọn để duy trì sau khi reload
        Conversation selectedConv = conversationListView.getSelectionModel().getSelectedItem();

        // Load conversations mới nhất từ cache (đã loại bỏ nhóm đã rời)
        conversations.setAll(Cache.getInstance().getConversationList());
        sortConversations();

        // Cố gắng chọn lại Conversation cũ (nếu nó vẫn còn)
        if (selectedConv != null) {
            conversations.stream()
                    .filter(c -> c.getId().equals(selectedConv.getId()))
                    .findFirst()
                    .ifPresent(c -> conversationListView.getSelectionModel().select(c));
        }
    }

    @Override
    public void setupEventBus() {
        // Subscribe to new conversation event
        unsubscribeNewConversation = MessageBus.subscribe(NewConversationEvent.class, this::handleNewConversationEvent);

        // Đăng ký cho các sự kiện cập nhật tin nhắn
        // Sử dụng lambda expression (event -> Platform.runLater(this::refreshListUI))

        messageUpdatesUnsubscribers.add(
                MessageBus.subscribe(MessageReceivedEvent.class, event -> Platform.runLater(this::refreshListUI))
        );
        messageUpdatesUnsubscribers.add(
                MessageBus.subscribe(MessageSendSuccessEvent.class, event -> Platform.runLater(this::refreshListUI))
        );
        messageUpdatesUnsubscribers.add(
                MessageBus.subscribe(MessageSendFailedEvent.class, event -> Platform.runLater(this::refreshListUI))
        );
    }

    private void handleNewConversationEvent(NewConversationEvent event) {
        // Khi nhận sự kiện NewConversationEvent, chúng ta phải TẢI LẠI TOÀN BỘ DANH SÁCH.
        Platform.runLater(this::loadData);
    }

    /**
     * Buộc ListView cập nhật UI bằng cách sắp xếp lại danh sách.
     */
    public void refreshListUI() {
        // Lấy danh sách hiện tại và set lại để kích hoạt cơ chế cập nhật của ListView Cell Factory
        List<Conversation> currentList = new ArrayList<>(conversations);
        conversations.setAll(currentList);
        sortConversations();

        // Vẫn cần cố gắng chọn lại item đang được chọn sau khi refresh
        Conversation selectedConv = conversationListView.getSelectionModel().getSelectedItem();
        if (selectedConv != null) {
            conversationListView.getSelectionModel().select(selectedConv);
        }
    }

    private void sortConversations() {
        conversations.sort(Comparator.comparing(Conversation::getName));
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if (unsubscribeNewConversation != null) {
            unsubscribeNewConversation.run();
        }
        // Hủy đăng ký tất cả các MessageBus listeners
        if (messageUpdatesUnsubscribers != null) {
            for (Runnable r : messageUpdatesUnsubscribers) {
                r.run();
            }
            messageUpdatesUnsubscribers.clear();
        }
    }
}