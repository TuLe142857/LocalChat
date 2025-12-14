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
import java.util.function.Function;

public class ChatListItem extends ListCell<Conversation> {

    private final HBox rootContent;
    private final VBox textContent;
    private final Label nameLabel;
    private final Label detailLabel;
    private final Circle avatarCircle;
    private final Label badgeLabel; // Badge for unread/pending

    // YÊU CẦU MỚI: Callback để lấy unread count từ client state
    private final Function<String, Integer> getUnreadCountCallback;

    public ChatListItem(Function<String, Integer> getUnreadCountCallback) {
        super();
        this.getUnreadCountCallback = getUnreadCountCallback;

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
                "-fx-background-color: #e74c3c; " + // Default Red
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

            // Lấy số tin nhắn chưa đọc từ client state (ChatLayout)
            int unreadCount = getUnreadCountCallback.apply(conversation.getId());

            // GIỮ LẠI: Đếm số tin nhắn chưa thành công (Pending/Failed)
            int pendingCount = conversation.getPendingMessage().size() + conversation.getFailedMessage().size();

            // Text detail
            if (conversation instanceof GroupConversation) {
                GroupConversation group = (GroupConversation) conversation;
                detailLabel.setText(String.format("%s (%d members)", type, group.getParticipantList().size()));
                avatarCircle.setStyle("-fx-fill: #9b59b6;"); // Purple for groups
            } else {
                detailLabel.setText(type);
                avatarCircle.setStyle("-fx-fill: #3498db;"); // Blue for direct
            }

            // Xử lý badge: Ưu tiên hiển thị tin nhắn chưa đọc (Dấu đỏ)
            if (unreadCount > 0) {
                badgeLabel.setText(String.valueOf(unreadCount));
                badgeLabel.setVisible(true);
                badgeLabel.setManaged(true);
                // Dùng màu đỏ đậm cho tin nhắn chưa đọc (như dấu đỏ)
                badgeLabel.setStyle(
                        "-fx-background-color: #e74c3c; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 10; " +
                                "-fx-font-size: 10px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 2 5;"
                );
                // Highlight text/background
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                detailLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                if (!isSelected()) {
                    // Nền sáng nhẹ khi có tin nhắn chưa đọc
                    setStyle("-fx-background-color: #f0f4f9;");
                } else {
                    setStyle("-fx-background-color: #dbe4ed;");
                }

            } else if (pendingCount > 0) {
                // Nếu không có tin nhắn chưa đọc, hiển thị số tin nhắn Pending/Failed
                badgeLabel.setText(String.valueOf(pendingCount));
                badgeLabel.setVisible(true);
                badgeLabel.setManaged(true);
                // Dùng màu cam/vàng nhẹ cho tin nhắn lỗi/pending
                badgeLabel.setStyle(
                        "-fx-background-color: #f39c12; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 10; " +
                                "-fx-font-size: 10px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 2 5;"
                );

                if (!isSelected()) {
                    setStyle("-fx-background-color: #e0f7fa;");
                } else {
                    setStyle("-fx-background-color: #dbe4ed;");
                }

            } else {
                // Không có gì cần chú ý
                badgeLabel.setVisible(false);
                badgeLabel.setManaged(false);

                // Xử lý selected state
                if (isSelected()) {
                    setStyle("-fx-background-color: #dbe4ed;"); // Light blue for selected
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                    detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #34495e;");
                } else {
                    setStyle("-fx-background-color: transparent;");
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
                }
            }

            // Đảm bảo style text được reset nếu không có unread count hoặc pending count
            if (unreadCount == 0 && pendingCount == 0) {
                if (isSelected()) {
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                    detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #34495e;");
                } else {
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
                }
            }

            setGraphic(rootContent);
        }
    }
}