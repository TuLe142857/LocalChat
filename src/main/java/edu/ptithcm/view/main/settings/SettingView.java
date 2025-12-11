package edu.ptithcm.view.main.settings;

import edu.ptithcm.view.base.BaseView;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class SettingView extends BaseView {

    @Override
    protected void init() {
    }

    @Override
    protected void setupUI() {
        Label label = new Label("⚙️ Cài đặt ứng dụng (Chức năng chưa triển khai)");
        label.setStyle("-fx-font-size: 1.5em; -fx-text-fill: #555;");

        StackPane.setAlignment(label, Pos.CENTER);
        this.getChildren().add(label);
    }

    @Override
    public void loadData() {
    }

    @Override
    public void setupEventBus() {
    }

    @Override
    public void onRemove() {
        super.onRemove();
    }
}