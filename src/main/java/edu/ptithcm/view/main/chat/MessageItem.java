package edu.ptithcm.view.main.chat;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Message;
import edu.ptithcm.view.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class MessageItem extends ListCell<Message> {

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
            setText(null);
            setStyle("");
            return;
        }

        String myId = Cache.getInstance().getCredential().getId();
        boolean isMine = message.getSenderId().equals(myId);

        HBox messageWrapper = new HBox();
        TextFlow messageTextFlow = new TextFlow();
        messageTextFlow.setPadding(new Insets(5));

        String senderName = isMine ? "You" : (Cache.getInstance().getPeer(message.getSenderId()) != null ? Cache.getInstance().getPeer(message.getSenderId()).getName() : "Unknown");

        VBox messageContent = new VBox();
        messageContent.setSpacing(2);

        if (isMine) {
            messageWrapper.setAlignment(Pos.CENTER_RIGHT);
            messageTextFlow.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-max-width: 80%;");
        } else {
            messageWrapper.setAlignment(Pos.CENTER_LEFT);
            messageTextFlow.setStyle("-fx-background-color: #E8E8E8; -fx-background-radius: 10; -fx-max-width: 80%;");
        }

        // 1. Sender Name (Chỉ hiện cho group chat và không phải tin của mình)
        if (!isMine && Cache.getInstance().getConversation(message.getConversationId()) instanceof edu.ptithcm.model.GroupConversation) {
            Text name = new Text(senderName + "\n");
            name.setStyle("-fx-font-weight: bold; -fx-fill: #3b5998; -fx-font-size: 0.8em;"); // Blue color for name
            messageTextFlow.getChildren().add(name);
        }

        // 2. Content
        Text content = new Text(message.getContent());
        messageTextFlow.getChildren().add(content);

        // 3. Status Icon (Chỉ hiện cho tin nhắn đi)
        if (isMine) {
            HBox statusBox = new HBox();
            statusBox.setPadding(new Insets(0, 0, 0, 5));
            statusBox.setAlignment(Pos.BOTTOM_RIGHT);
            statusBox.getChildren().add(UIUtils.getMessageStatusIcon(message.getStatus()));

            messageWrapper.getChildren().add(messageTextFlow);
            messageWrapper.getChildren().add(statusBox);
        } else {
            messageWrapper.getChildren().add(messageTextFlow);
        }

        messageWrapper.setPadding(new Insets(2, 5, 2, 5));
        messageWrapper.setMaxWidth(Double.MAX_VALUE);
        setGraphic(messageWrapper);
    }
}