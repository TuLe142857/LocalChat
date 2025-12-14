package edu.ptithcm.view;

import edu.ptithcm.model.Message;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UIUtils {

    // Bộ ánh xạ cho các biểu tượng cảm xúc tùy chỉnh
    private static final Pattern EMOJI_PATTERN = Pattern.compile("(:v|:\\)|:\\)\\)|:D|:\\(|:'\\(|<3|:O|:P|T_T)");
    private static final String EMOJI_FONT_SIZE = "-fx-font-size: 1.2em;";

    /**
     * Tạo icon hiển thị trạng thái tin nhắn.
     * ... (Giữ nguyên logic này) ...
     */
    public static Node getMessageStatusIcon(Message.MessageStatus status) {
        HBox iconWrapper = new HBox();
        iconWrapper.setStyle("-fx-min-width: 10px; -fx-min-height: 10px;");

        if (status == null) {
            return iconWrapper;
        }

        switch (status) {
            case PENDING:
                Circle pending = new Circle(3);
                pending.setFill(Color.GRAY);
                iconWrapper.getChildren().add(pending);
                return iconWrapper;
            case SUCCESS:
                Circle success = new Circle(3);
                success.setFill(Color.web("#4CAF50"));
                iconWrapper.getChildren().add(success);
                return iconWrapper;
            case FAILED:
                Text failed = new Text("✕");
                failed.setFont(Font.font("Arial", 8));
                failed.setFill(Color.RED);
                iconWrapper.getChildren().add(failed);
                return iconWrapper;
            default:
                return iconWrapper;
        }
    }

    /**
     * Phân tích nội dung tin nhắn, thay thế các chuỗi ký tự thành biểu tượng cảm xúc (emoji).
     * @param content Nội dung tin nhắn thô.
     * @return TextFlow chứa Text nodes và Text nodes của emoji.
     */
    public static TextFlow convertToEmojiTextFlow(String content) {
        TextFlow textFlow = new TextFlow();
        Matcher matcher = EMOJI_PATTERN.matcher(content);
        int lastAppendPosition = 0;

        while (matcher.find()) {
            // 1. Thêm đoạn text trước biểu tượng cảm xúc
            if (matcher.start() > lastAppendPosition) {
                String precedingText = content.substring(lastAppendPosition, matcher.start());
                // QUAN TRỌNG: Thêm text node bình thường
                textFlow.getChildren().add(new Text(precedingText));
            }

            // 2. Thêm biểu tượng cảm xúc
            String emojiText = matcher.group();
            Text emojiNode = new Text(mapTextToEmoji(emojiText));
            emojiNode.setStyle(EMOJI_FONT_SIZE); // Làm cho emoji nổi bật hơn
            textFlow.getChildren().add(emojiNode);

            lastAppendPosition = matcher.end();
        }

        // 3. Thêm phần còn lại của chuỗi
        if (lastAppendPosition < content.length()) {
            // QUAN TRỌNG: Thêm text node bình thường
            textFlow.getChildren().add(new Text(content.substring(lastAppendPosition)));
        }

        return textFlow;
    }

    /**
     * Ánh xạ chuỗi ký tự thành ký tự Unicode Emoji.
     * @param text
     * @return
     */
    private static String mapTextToEmoji(String text) {
        switch (text) {
            case ":v":
                return "😅";
            case ":)":
                return "😊";
            case ":))":
                return "😄";
            case ":D":
                return "😀";
            case ":(":
                return "😞";
            case ":'(":
                return "😢";
            case "<3":
                return "❤️";
            case ":O":
                return "😮";
            case ":P":
                return "😛";
            case "T_T":
                return "😭";
            default:
                return text;
        }
    }
}