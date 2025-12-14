package edu.ptithcm.view.main;

import edu.ptithcm.model.Peer;
import edu.ptithcm.view.base.BaseView;
import edu.ptithcm.view.main.chat.ChatLayout;
import edu.ptithcm.view.main.search.SearchView;
import javafx.scene.layout.BorderPane;

public class MainLayout extends BaseView {

    private BorderPane rootLayout;
    private BaseView currentContentView;
    private Runnable onLogoutRequest;

    private ChatLayout chatLayoutInstance;

    public MainLayout(Runnable onLogoutRequest){
        this.onLogoutRequest = onLogoutRequest;
    }

    public MainLayout() {
        this(null);
    }

    @Override protected void init() {
        chatLayoutInstance = new ChatLayout(null);
    }

    @Override
    protected void setupUI() {
        rootLayout = new BorderPane();

        SidebarView sidebar = new SidebarView(this::switchContent);

        rootLayout.setLeft(sidebar);

        switchContent("CHAT");

        this.getChildren().add(rootLayout);
    }

    // Callback truyền vào SearchView. Hàm này là Consumer<Peer>
    private void startDirectChatFromSearch(Peer peer) {
        switchContent("CHAT");

        if (chatLayoutInstance != null) {
            chatLayoutInstance.startDirectChat(peer);
        }
    }


    private void switchContent(String viewName) {
        if (currentContentView != null) {
            currentContentView.onRemove();
        }

        switch (viewName) {
            case "CHAT":
                currentContentView = chatLayoutInstance;
                break;
            case "SEARCH":
                // Gọi constructor của SearchView bằng lambda (Consumer<Peer>)
                currentContentView = new SearchView(this::startDirectChatFromSearch);
                break;
            case "LOGOUT":
                if(onLogoutRequest != null) onLogoutRequest.run();
                return;
            default:
                return;
        }

        currentContentView.loadData();

        rootLayout.setCenter(currentContentView);
    }

    @Override public void loadData() {}
    @Override public void setupEventBus() {}

    @Override public void onRemove() {
        super.onRemove();
        if (chatLayoutInstance != null) {
            chatLayoutInstance.onRemove();
        }
    }
}