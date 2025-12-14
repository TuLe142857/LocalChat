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
import javafx.scene.layout.Region;
import edu.ptithcm.model.GroupConversation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

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

        // Content VBox (Name + TextFlow + Status)
        VBox contentBox = new VBox(2); // VBox wraps name and bubble

        // 1. Sender Name Label (Outside the bubble, for group chat not mine)
        boolean isGroupChat = Cache.getInstance().getConversation(message.getConversationId()) instanceof GroupConversation;
        String senderName = Cache.getInstance().getPeer(message.getSenderId()) != null
                ? Cache.getInstance().getPeer(message.getSenderId()).getName()
                : "Unknown";

        if (!isMine && isGroupChat) {
            Label senderNameLabel = new Label(senderName);
            senderNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 0.85em; -fx-padding: 0 0 2 0;");
            contentBox.getChildren().add(senderNameLabel);
        }

        // 2. Message Text Content (The Bubble)
        TextFlow messageTextFlow = UIUtils.convertToEmojiTextFlow(message.getContent());
        messageTextFlow.setPadding(new Insets(8, 12, 8, 12)); // Thêm padding cho bubble

        // 3. Status Icon (Chỉ hiện cho tin nhắn đi)
        Node statusIcon = UIUtils.getMessageStatusIcon(message.getStatus());
        HBox statusBox = new HBox(statusIcon);
        statusBox.setAlignment(Pos.BOTTOM_RIGHT);
        statusBox.setPadding(new Insets(0, 0, 0, 5));

        // Horizontal wrapper for bubble and status
        HBox bubbleAndStatus = new HBox(5);
        bubbleAndStatus.getChildren().add(messageTextFlow);

        if (isMine) {
            // Tin nhắn của tôi (phải)
            messageTextFlow.setStyle("-fx-background-color: #3498db; -fx-background-radius: 15 15 0 15;");
            contentBox.setAlignment(Pos.CENTER_RIGHT);
            bubbleAndStatus.setAlignment(Pos.BOTTOM_RIGHT);

            // Đặt text thành màu trắng
            for(Node node : messageTextFlow.getChildren()) {
                if (node instanceof Text) {
                    ((Text) node).setFill(Color.WHITE);
                }
            }

            bubbleAndStatus.getChildren().add(statusBox);

        } else {
            // Tin nhắn của người khác (trái)
            messageTextFlow.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 15 15 15 0;");
            contentBox.setAlignment(Pos.CENTER_LEFT);
            bubbleAndStatus.setAlignment(Pos.BOTTOM_LEFT);

            // Đặt text thành màu đen (mặc định)
            for(Node node : messageTextFlow.getChildren()) {
                if (node instanceof Text) {
                    ((Text) node).setFill(Color.BLACK);
                }
            }
        }

        contentBox.getChildren().add(bubbleAndStatus);

        // 4. Main wrapper
        HBox messageWrapper = new HBox(contentBox);
        messageWrapper.setPadding(new Insets(2, 5, 2, 5));
        messageWrapper.setMaxWidth(Double.MAX_VALUE);
        messageWrapper.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // ĐẶT GIỚI HẠN CHIỀU RỘNG TỐI ĐA
        if (getListView() != null) {
            double maxWidth = getListView().getWidth() * 0.65; // 65% width
            messageTextFlow.setMaxWidth(maxWidth);
            contentBox.setMaxWidth(maxWidth);
        }

        setGraphic(messageWrapper);
    }
}