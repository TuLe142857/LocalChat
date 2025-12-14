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
import java.util.function.Consumer;

public class ChatListView extends BaseView {
    private ListView<Conversation> conversationListView;
    private ObservableList<Conversation> conversations;
    private Runnable unsubscribeNewConversation;

    // ĐÃ SỬA: Bỏ 'final' và chuyển khởi tạo sang init()
    private List<Runnable> messageUpdatesUnsubscribers;

    private final Consumer<Conversation> onConversationSelected;

    public ChatListView(Consumer<Conversation> onConversationSelected) {
        this.onConversationSelected = onConversationSelected;
    }

    @Override
    protected void init() {
        conversations = FXCollections.observableArrayList();
        // ĐÃ SỬA: Khởi tạo danh sách ở đây
        messageUpdatesUnsubscribers = new ArrayList<>();
    }

    @Override
    protected void setupUI() {
        conversationListView = new ListView<>(conversations);
        conversationListView.setCellFactory(param -> new ChatListItem());

        // Handle selection event
        conversationListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        onConversationSelected.accept(newVal);
                    }
                });

        VBox.setVgrow(conversationListView, Priority.ALWAYS);

        this.getChildren().add(conversationListView);
    }

    @Override
    public void loadData() {
        // Load initial conversations from cache
        conversations.setAll(Cache.getInstance().getConversationList());
        sortConversations();
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
        Platform.runLater(() -> {
            Conversation newConv = Cache.getInstance().getConversation(event.getConversationId());
            if (newConv != null) {
                if (!conversations.contains(newConv)) {
                    conversations.add(newConv);
                    sortConversations();
                }
            }
        });
    }

    /**
     * Buộc ListView cập nhật UI bằng cách sắp xếp lại danh sách.
     */
    private void refreshListUI() {
        // Lấy danh sách hiện tại và set lại để kích hoạt cơ chế cập nhật của ListView Cell Factory
        List<Conversation> currentList = new ArrayList<>(conversations);
        conversations.setAll(currentList);
        sortConversations();
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