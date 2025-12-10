package edu.ptithcm.view.main.chat;

import edu.ptithcm.view.base.BaseView;
import javafx.scene.control.SplitPane;

public class ChatLayout extends BaseView {

    @Override
    protected void init() {

    }

    @Override
    protected void setupUI() {
        // Bên trái: Danh sách người dùng
        // Bên phải: Khung chat
        // (Bạn có thể tách 2 cái này ra thành ChatListPart và ChatBoxPart nếu phức tạp)

        BaseView chatList = new ChatListView(); // Component con
        BaseView chatBox = new ChatBoxView();   // Component con

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(chatList, chatBox);
        splitPane.setDividerPositions(0.3f); // 30% cho list

        this.getChildren().add(splitPane);
    }

    @Override
    public void loadData() {
        // ChatLayout là container nên có thể không load data,
        // để 2 thằng con (List, Box) tự load.
    }

    @Override
    public void setupEventBus() {
        // Subscribe các sự kiện chung nếu cần
    }

    @Override
    public void onRemove() {
        super.onRemove();
        // Khi remove Layout cha, phải đảm bảo các view con cũng được dọn dẹp
        // JavaFX không tự gọi onRemove cho con, ta phải tự gọi nếu cần thiết
        // (Hoặc để các view con tự handle khi chúng bị gỡ khỏi Scene graph nếu dùng listener)
    }
}