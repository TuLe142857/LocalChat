package edu.ptithcm.view.main.chat;

import edu.ptithcm.model.Conversation;
import edu.ptithcm.model.DirectConversation;
import edu.ptithcm.model.GroupConversation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class ChatListItem extends ListCell<Conversation> {

    private final HBox rootContent;
    private final VBox textContent;
    private final Label nameLabel;
    private final Label detailLabel;
    private final Circle avatarCircle;
    private final Label badgeLabel; // Badge for unread/pending

    public ChatListItem() {
        super();

        // 1. Avatar (Placeholder)
        avatarCircle = new Circle(18);
        avatarCircle.setStyle("-fx-fill: #3498db;");

        // 2. Text Content (Name & Detail)
        nameLabel = new Label();
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        detailLabel = new Label();
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;"); // Darker gray for subtlety

        textContent = new VBox(nameLabel, detailLabel);
        textContent.setSpacing(1);
        HBox.setHgrow(textContent, Priority.ALWAYS); // Let text take available space

        // 3. Badge (Unread/Pending Count)
        badgeLabel = new Label();
        badgeLabel.setMinWidth(20);
        badgeLabel.setAlignment(Pos.CENTER);
        badgeLabel.setStyle(
                "-fx-background-color: #e74c3c; " + // Red for attention
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 2 5;"
        );
        badgeLabel.setVisible(false);
        badgeLabel.setManaged(false);

        // 4. Root Layout
        rootContent = new HBox(10, avatarCircle, textContent, badgeLabel);
        rootContent.setAlignment(Pos.CENTER_LEFT);
        rootContent.setPadding(new Insets(10, 15, 10, 15));
        rootContent.setMaxWidth(Double.MAX_VALUE);

        // Default style for the cell itself
        this.setStyle("-fx-background-color: transparent;");

        // Selection style override
        this.getStyleClass().add("chat-list-item");
        this.setOnMouseEntered(e -> {
            if (!isSelected()) setStyle("-fx-background-color: #f0f4f9;");
        });
        this.setOnMouseExited(e -> {
            if (!isSelected()) setStyle("-fx-background-color: transparent;");
        });
    }

    @Override
    protected void updateItem(Conversation conversation, boolean empty) {
        super.updateItem(conversation, empty);

        if (empty || conversation == null) {
            setGraphic(null);
            setText(null);
            setStyle("-fx-background-color: transparent;");
        } else {
            nameLabel.setText(conversation.getName());

            String type = (conversation instanceof DirectConversation) ? "Direct Chat" : "Group Chat";

            // Đếm số tin nhắn chưa thành công (Pending/Failed)
            int attentionCount = conversation.getPendingMessage().size() + conversation.getFailedMessage().size();

            if (conversation instanceof GroupConversation) {
                GroupConversation group = (GroupConversation) conversation;
                detailLabel.setText(String.format("%s (%d members)", type, group.getParticipantList().size()));
                avatarCircle.setStyle("-fx-fill: #9b59b6;"); // Purple for groups
            } else {
                detailLabel.setText(type);
                avatarCircle.setStyle("-fx-fill: #3498db;"); // Blue for direct
            }

            // Xử lý badge
            if (attentionCount > 0) {
                badgeLabel.setText(String.valueOf(attentionCount));
                badgeLabel.setVisible(true);
                badgeLabel.setManaged(true);
                // Dùng màu vàng nhẹ khi có tin lỗi/pending
                if (isSelected()) {
                    setStyle("-fx-background-color: #dbe4ed;");
                } else {
                    setStyle("-fx-background-color: #e0f7fa;");
                }
            } else {
                badgeLabel.setVisible(false);
                badgeLabel.setManaged(false);
                if (isSelected()) {
                    setStyle("-fx-background-color: #dbe4ed;"); // Light blue for selected
                } else {
                    setStyle("-fx-background-color: transparent;");
                }
            }

            // Xử lý selected state
            if (isSelected()) {
                setStyle("-fx-background-color: #dbe4ed;");
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #34495e;");
            } else {
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
            }

            setGraphic(rootContent);
        }
    }
}