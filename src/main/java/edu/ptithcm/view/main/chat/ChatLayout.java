package edu.ptithcm.view.main.chat;

import edu.ptithcm.view.base.BaseView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class ChatLayout extends BaseView {

    private ChatListView chatList;
    private ChatBoxView chatBox;

    @Override
    protected void init() {

    }

    @Override
    protected void setupUI() {

        chatBox = new ChatBoxView();

        // Truyền callback để khi chọn Conversation bên list sẽ gọi setConversation bên box
        chatList = new ChatListView(chatBox::setConversation);

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
        // Khi remove Layout cha, phải đảm bảo các view con cũng được dọn dẹp
        chatList.onRemove();
        chatBox.onRemove();
    }
}