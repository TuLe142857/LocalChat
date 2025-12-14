package edu.ptithcm.view.main.chat;

import edu.ptithcm.model.GroupConversation;
import edu.ptithcm.model.Peer;
import edu.ptithcm.view.base.BaseView;
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

    public GroupMemberView(GroupConversation group) {
        this.group = group;
        this.stage = new Stage();
    }

    @Override
    protected void init() {
        members = FXCollections.observableArrayList(group.getParticipantList());

        stage.initModality(Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ chính
        stage.setTitle("Group Members: " + group.getName());
        stage.setResizable(false);
    }

    @Override
    protected void setupUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Tiêu đề
        Label titleLabel = new Label("Members (" + members.size() + ")");
        titleLabel.setStyle("-fx-font-size: 1.2em; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        root.setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        // Center: Danh sách thành viên (có thể tái sử dụng PeerListItem nếu cần)
        memberListView = new ListView<>(members);
        // Tùy chỉnh Cell Factory nếu muốn hiển thị chi tiết hơn (hiện tại dùng toString)
        // memberListView.setCellFactory(param -> new PeerListItem());
        memberListView.setPrefSize(350, 400);
        root.setCenter(memberListView);

        // Bottom: Nút đóng
        Button closeButton = new Button("Close");
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.setOnAction(e -> stage.close());

        VBox bottomBox = new VBox(closeButton);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(bottomBox);

        // Thiết lập Scene và Stage
        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    // Phương thức công khai để hiển thị cửa sổ
    public void show() {
        // Cập nhật lại danh sách ngay trước khi hiển thị (nếu có thay đổi real-time)
        members.setAll(group.getParticipantList());
        stage.showAndWait();
    }

    @Override public void loadData() {}
    @Override public void setupEventBus() {} // Không cần thiết lập EventBus cho view modal đơn giản này

    @Override
    public void onRemove() {
        // Không cần dọn dẹp gì đặc biệt khi đóng cửa sổ
    }
}