package edu.ptithcm.view.main.chat;

import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.GroupConversation;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class ChatListItem extends ListCell<Conversation> {
    private final VBox content;
    private final Label nameLabel;
    private final Label detailLabel;

    public ChatListItem() {
        super();
        nameLabel = new Label();
        nameLabel.setStyle("-fx-font-weight: bold;");

        detailLabel = new Label();
        detailLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        content = new VBox(nameLabel, detailLabel);
        content.setSpacing(2);
        content.setPadding(new Insets(5, 10, 5, 10));
        this.setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void updateItem(Conversation conversation, boolean empty) {
        super.updateItem(conversation, empty);

        if (empty || conversation == null) {
            setGraphic(null);
            setText(null);
        } else {
            nameLabel.setText(conversation.getName());

            String type = (conversation instanceof DirectConversation) ? "Direct Chat" : "Group Chat";

            // Đếm số tin nhắn chưa thành công (Pending/Failed)
            int attentionCount = conversation.getPendingMessage().size() + conversation.getFailedMessage().size();

            if (conversation instanceof GroupConversation) {
                GroupConversation group = (GroupConversation) conversation;
                detailLabel.setText(String.format("%s (%d members)", type, group.getParticipantList().size()));
            } else {
                detailLabel.setText(type);
            }

            if (attentionCount > 0) {
                // Đánh dấu cần chú ý
                this.setStyle("-fx-background-color: #e0f7fa;"); // Màu xanh nhạt để làm nổi bật
            } else {
                this.setStyle("-fx-background-color: transparent;");
            }
            setGraphic(content);
        }
    }
}