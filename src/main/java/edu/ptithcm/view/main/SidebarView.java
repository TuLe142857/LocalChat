package edu.ptithcm.view.main;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

// Sidebar không cần là BaseView nếu nó static, nhưng kế thừa VBox cho tiện
public class SidebarView extends VBox {

    public SidebarView(Consumer<String> onMenuSelected) {
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #ddd; -fx-pref-width: 200;");

        // Avatar area (Placeholder)
        Button btnAccount = new Button("Account Info");

        // Menu Items
        Button btnChat = new Button("Chat");
        btnChat.setOnAction(e -> onMenuSelected.accept("CHAT"));

        Button btnSearch = new Button("Search Peer");
        btnSearch.setOnAction(e -> onMenuSelected.accept("SEARCH"));

        Button btnSetting = new Button("Settings");
        btnSetting.setOnAction(e -> onMenuSelected.accept("SETTING"));

        Button btnLogout = new Button("Logout");
        btnLogout.setOnAction(e->onMenuSelected.accept("LOGOUT"));
        // Logic logout sẽ xử lý ở tầng trên hoặc bắn event

        this.getChildren().addAll(btnAccount, btnChat, btnSearch, btnSetting, btnLogout);
    }
}