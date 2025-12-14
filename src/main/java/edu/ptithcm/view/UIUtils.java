package edu.ptithcm.view;

import edu.ptithcm.model.Message;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class UIUtils {

    /**
     * Tạo icon hiển thị trạng thái tin nhắn.
     * PENDING: Vòng tròn xám nhỏ (chờ gửi)
     * SUCCESS: Vòng tròn xanh lá (đã gửi/đã nhận)
     * FAILED: Ký tự X màu đỏ (gửi thất bại)
     * @param status
     * @return
     */
    public static Node getMessageStatusIcon(Message.MessageStatus status) {
        HBox iconWrapper = new HBox();
        iconWrapper.setStyle("-fx-min-width: 10px; -fx-min-height: 10px;"); // Cố định kích thước

        if (status == null) {
            return iconWrapper;
        }

        switch (status) {
            case PENDING:
                // Dùng Circle màu xám
                Circle pending = new Circle(3);
                pending.setFill(Color.GRAY);
                iconWrapper.getChildren().add(pending);
                return iconWrapper;
            case SUCCESS:
                // Dùng Circle màu xanh
                Circle success = new Circle(3);
                success.setFill(Color.web("#4CAF50")); // Green
                iconWrapper.getChildren().add(success);
                return iconWrapper;
            case FAILED:
                // Dùng Text '✕' màu đỏ
                Text failed = new Text("✕");
                failed.setFont(Font.font("Arial", 8));
                failed.setFill(Color.RED);
                iconWrapper.getChildren().add(failed);
                return iconWrapper;
            default:
                return iconWrapper;
        }
    }
}