package edu.ptithcm.view.main;

import edu.ptithcm.view.base.BaseView;
import edu.ptithcm.view.main.chat.ChatLayout;
import edu.ptithcm.view.main.search.SearchView;
import javafx.scene.layout.BorderPane;

public class MainLayout extends BaseView {

    private BorderPane rootLayout;
    private BaseView currentContentView; // Để gọi onRemove khi switch tab
    private Runnable onLogoutRequest;
    public MainLayout(Runnable onLogoutRequest){
        this.onLogoutRequest = onLogoutRequest;
    }

    @Override
    protected void init() {

    }

    @Override
    protected void setupUI() {
        rootLayout = new BorderPane();

        // Tạo Sidebar và truyền callback xử lý khi user click menu
        SidebarView sidebar = new SidebarView(this::switchContent);

        rootLayout.setLeft(sidebar);

        // Mặc định hiện Chat
        switchContent("CHAT");

        this.getChildren().add(rootLayout);
    }

    // Hàm chuyển đổi nội dung ở giữa (Content Bar)
    private void switchContent(String viewName) {
        // 1. Dọn dẹp view cũ (QUAN TRỌNG: Unsubscribe Message Bus)
        if (currentContentView != null) {
            currentContentView.onRemove();
        }

        // 2. Tạo view mới
        switch (viewName) {
            case "CHAT":
                currentContentView = new ChatLayout();
                break;
            case "SEARCH":
                currentContentView = new SearchView(); // Giả sử đã tạo class này
                break;
            case "SETTING":
                // currentContentView = new SettingView();
                break;
            case "LOGOUT":
                onLogoutRequest.run();
                break;
            default:
                return;
        }

        // 3. Gắn vào Center
        rootLayout.setCenter(currentContentView);
    }

    @Override public void loadData() {}
    @Override public void setupEventBus() {}
}