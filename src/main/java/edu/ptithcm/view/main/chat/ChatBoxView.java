package edu.ptithcm.view.main.chat;

import edu.ptithcm.view.base.BaseView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class ChatBoxView extends BaseView {

    private TextArea messageArea;

    @Override
    protected void init() {

    }

    @Override
    protected void setupUI() {
        BorderPane layout = new BorderPane();

        messageArea = new TextArea();
        messageArea.setEditable(false);

        TextField inputField = new TextField();
        inputField.setPromptText("Nhập tin nhắn...");

        layout.setCenter(messageArea);
        layout.setBottom(inputField);

        this.getChildren().add(layout);
    }

    @Override
    public void loadData() {
        // Load lịch sử chat từ cache local
        messageArea.setText("Loading history...");
    }

    @Override
    public void setupEventBus() {
        // Giả sử: MessageBus.subscribe("NEW_MESSAGE", this::onNewMessage);
        System.out.println("ChatBox: Subscribed to NEW_MESSAGE");
    }

    @Override
    public void onRemove() {
        super.onRemove();
        // MessageBus.unsubscribe("NEW_MESSAGE", this::onNewMessage);
        System.out.println("ChatBox: Unsubscribed from NEW_MESSAGE");
    }
}