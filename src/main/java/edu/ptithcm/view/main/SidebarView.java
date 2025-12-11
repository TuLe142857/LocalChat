package edu.ptithcm.view.main;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class SidebarView extends VBox {

    public SidebarView(Consumer<String> onMenuSelected) {
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        // Style: nền tối, chiều rộng cố định
        this.setStyle("-fx-background-color: #3f51b5; -fx-pref-width: 200;");

        // Avatar area: Giả định Cache có getCredential().getName()
        Button btnAccount = new Button("👤 " + edu.ptithcm.cache.Cache.getInstance().getCredential().getName());
        btnAccount.setMaxWidth(Double.MAX_VALUE);
        btnAccount.setStyle("-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15 10 15 10; -fx-border-color: #5c6bc0; -fx-border-width: 0 0 1 0;");

        // Menu Styles
        String menuStyle = "-fx-background-color: transparent; -fx-text-fill: #e8eaf6; -fx-alignment: center-left; -fx-font-size: 1.1em; -fx-padding: 10;";
        String menuHoverStyle = "-fx-background-color: #5c6bc0; -fx-text-fill: white; -fx-padding: 10;";

        Button btnChat = new Button("💬 Đoạn Chat");
        btnChat.setMaxWidth(Double.MAX_VALUE);
        btnChat.setStyle(menuStyle);
        btnChat.setOnAction(e -> onMenuSelected.accept("CHAT"));

        Button btnSearch = new Button("🔎 Tìm Peer");
        btnSearch.setMaxWidth(Double.MAX_VALUE);
        btnSearch.setStyle(menuStyle);
        btnSearch.setOnAction(e -> onMenuSelected.accept("SEARCH"));

        Button btnSetting = new Button("⚙️ Cài đặt");
        btnSetting.setMaxWidth(Double.MAX_VALUE);
        btnSetting.setStyle(menuStyle);
        btnSetting.setOnAction(e -> onMenuSelected.accept("SETTING"));

        // Add hover effect
        for (Button btn : new Button[]{btnChat, btnSearch, btnSetting}) {
            btn.setOnMouseEntered(e -> btn.setStyle(menuHoverStyle));
            btn.setOnMouseExited(e -> btn.setStyle(menuStyle));
        }

        // Spacer để đẩy Logout xuống dưới
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnLogout = new Button("🚪 Đăng xuất");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setOnAction(e->onMenuSelected.accept("LOGOUT"));
        btnLogout.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;");

        this.getChildren().addAll(btnAccount, btnChat, btnSearch, btnSetting, spacer, btnLogout);
    }
}