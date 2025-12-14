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
        btnAccount.setMaxWidth(Double.MAX_VALUE);

        // Menu Items
        Button btnChat = new Button("Chat");
        btnChat.setMaxWidth(Double.MAX_VALUE);
        btnChat.setOnAction(e -> onMenuSelected.accept("CHAT"));

        Button btnSearch = new Button("Search Peer / Group");
        btnSearch.setMaxWidth(Double.MAX_VALUE);
        btnSearch.setOnAction(e -> onMenuSelected.accept("SEARCH"));

        Button btnLogout = new Button("Logout");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setOnAction(e->onMenuSelected.accept("LOGOUT"));

        this.getChildren().addAll(btnAccount, btnChat, btnSearch, btnLogout);
    }
}