package edu.ptithcm.view.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import java.util.function.Consumer;

// Sidebar không cần là BaseView nếu nó static, nhưng kế thừa VBox cho tiện
public class SidebarView extends VBox {

    private void styleButton(Button button, String icon) {
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-family: 'Segoe UI Symbol', 'Arial'; -fx-font-size: 16px; -fx-text-fill: #ecf0f1;");

        button.setGraphic(iconLabel);
        button.setGraphicTextGap(10);

        button.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #ecf0f1; " + // Light text
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 15 10 15;"
        );

        // Hover effect: dùng lambda để tránh lỗi style chồng chéo
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #34495e; " + // Màu nền đậm hơn khi hover
                        "-fx-text-fill: #ecf0f1; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 15 10 15;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #ecf0f1; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 15 10 15;"
        ));
    }

    public SidebarView(Consumer<String> onMenuSelected) {
        this.setPadding(new Insets(20, 0, 10, 0));
        this.setSpacing(5);
        // Cấu hình để VBox (Sidebar) mở rộng hết chiều cao của container.
        this.setMaxHeight(Double.MAX_VALUE);
        // Màu nền tối hơn, hiện đại hơn
        this.setStyle("-fx-background-color: #2c3e50; -fx-pref-width: 200;");

        // KHU VỰC ACCOUNT INFO ĐÃ ĐƯỢC BỎ

        // Menu Items
        Button btnChat = new Button("Chat");
        styleButton(btnChat, "\uD83D\uDCAC"); // Icon: Chat Bubble
        btnChat.setOnAction(e -> onMenuSelected.accept("CHAT"));

        Button btnSearch = new Button("Search Peer / Group"); // Giữ nguyên text
        styleButton(btnSearch, "\uD83D\uDD0D"); // Icon: Magnifying Glass
        btnSearch.setOnAction(e -> onMenuSelected.accept("SEARCH"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("Logout");
        styleButton(btnLogout, "\u23FB"); // Icon: Logout
        btnLogout.setOnAction(e->onMenuSelected.accept("LOGOUT"));
        VBox.setMargin(btnLogout, new Insets(10, 0, 10, 0));

        // CHỈ GIỮ LẠI CÁC NÚT CẦN THIẾT
        this.getChildren().addAll(btnChat, btnSearch, spacer, btnLogout);
    }
}