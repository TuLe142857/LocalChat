package edu.ptithcm.view.main;

import edu.ptithcm.view.base.BaseView;
import edu.ptithcm.view.main.chat.ChatLayout;
import edu.ptithcm.view.main.search.SearchView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

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
        rootLayout.setStyle("-fx-background-color: #f5f5f5;"); // Thêm màu nền nhẹ cho layout chính

        // Tạo Sidebar và truyền callback xử lý khi user click menu
        SidebarView sidebar = new SidebarView(this::switchContent);

        // ĐÃ SỬA: Loại bỏ VBox wrapper và đặt SidebarView trực tiếp vào BorderPane.setLeft.
        // SidebarView (VBox) đã được cấu hình chiều rộng cố định (200) và màu nền tối bên trong nó.
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
        BaseView nextView = null;
        switch (viewName) {
            case "CHAT":
                nextView = new ChatLayout();
                break;
            case "SEARCH":
                nextView = new SearchView();
                break;
            // Removed "SETTING" case as requested
            case "LOGOUT":
                onLogoutRequest.run();
                return;
            default:
                return;
        }

        currentContentView = nextView;
        // 3. Gắn vào Center
        rootLayout.setCenter(currentContentView);
    }

    @Override public void loadData() {}
    @Override public void setupEventBus() {}

    @Override
    public void onRemove() {
        super.onRemove();
        if (currentContentView != null) {
            currentContentView.onRemove();
        }
    }
}