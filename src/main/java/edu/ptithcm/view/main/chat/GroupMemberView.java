package edu.ptithcm.view.main.chat;

import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.model.Peer;
import edu.ptithcm.view.base.BaseView;
import edu.ptithcm.view.main.search.PeerListItem; // CẦN IMPORT NÀY
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class GroupMemberView extends BaseView {

    private final GroupConversation group;
    private ListView<Peer> memberListView;
    private ObservableList<Peer> members;
    private final Stage stage;
    private Label titleLabel;
    private BorderPane rootLayout;

    public GroupMemberView(GroupConversation group) {
        this.group = group;
        this.stage = new Stage();

        // Setup Stage configuration
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Group Members");
        stage.setResizable(false);
    }

    @Override
    protected void init() {
        members = FXCollections.observableArrayList();
    }


    @Override
    protected void setupUI() {
        // ... (phần trên giữ nguyên)
        rootLayout = new BorderPane();
        rootLayout.setPadding(new Insets(10));

        // Top: Tiêu đề
        titleLabel = new Label("Members");
        titleLabel.setStyle("-fx-font-size: 1.2em; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        rootLayout.setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        // Center: Danh sách thành viên
        memberListView = new ListView<>(members);
        // ĐÃ SỬA: Sử dụng PeerListItem để hiển thị thông tin thân thiện hơn
        memberListView.setCellFactory(param -> new PeerListItem());
        memberListView.setPrefSize(350, 400);
        rootLayout.setCenter(memberListView);

        // ... (phần dưới giữ nguyên)

        // Bottom: Nút đóng
        Button closeButton = new Button("Close");
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.setOnAction(e -> stage.close());

        VBox bottomBox = new VBox(closeButton);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));
        rootLayout.setBottom(bottomBox);

        // Thêm layout vào BaseView (StackPane - là 'this')
        this.getChildren().add(rootLayout);
    }

    // Phương thức công khai để hiển thị cửa sổ
    public void show() {
        if (stage.getScene() == null) {
            stage.setScene(new Scene(this));
        }

        // 1. Cập nhật dữ liệu
        members.setAll(group.getParticipantList());

        // 2. Cập nhật tiêu đề
        stage.setTitle("Group Members: " + group.getName());
        titleLabel.setText("Members (" + members.size() + ")");

        stage.showAndWait();
    }

    @Override public void loadData() {}
    @Override public void setupEventBus() {}

    @Override
    public void onRemove() {
        // Không cần dọn dẹp gì đặc biệt khi đóng cửa sổ
    }
}