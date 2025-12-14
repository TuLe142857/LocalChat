package edu.ptithcm.view.main.search;

import edu.ptithcm.model.Peer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class PeerListItem extends ListCell<Peer> {
    private final HBox rootContent;
    private final VBox textContent;
    private final Label nameLabel;
    private final Label detailLabel;
    private final Circle statusIndicator;

    public PeerListItem() {
        super();

        // Status Indicator
        statusIndicator = new Circle(5);
        statusIndicator.setStyle("-fx-fill: #2ecc71;"); // Green for active peer (placeholder)

        nameLabel = new Label();
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        detailLabel = new Label();
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        textContent = new VBox(nameLabel, detailLabel);
        textContent.setSpacing(2);

        rootContent = new HBox(10, statusIndicator, textContent);
        rootContent.setAlignment(Pos.CENTER_LEFT);
        rootContent.setPadding(new Insets(10, 15, 10, 15));

        // Selection style
        this.setOnMouseEntered(e -> {
            if (!isSelected()) setStyle("-fx-background-color: #f0f4f9;");
        });
        this.setOnMouseExited(e -> {
            if (!isSelected()) setStyle("-fx-background-color: transparent;");
        });
    }

    @Override
    protected void updateItem(Peer peer, boolean empty) {
        super.updateItem(peer, empty);

        if (empty || peer == null) {
            setGraphic(null);
            setText(null);
        } else {
            nameLabel.setText(peer.getName());
            detailLabel.setText(String.format("IP: %s | ID: %s", peer.getIp().getHostAddress(), peer.getId().substring(0, 8) + "..."));

            // Assume the peer is active/known (since they are in the list)
            statusIndicator.setStyle("-fx-fill: #2ecc71;");

            setGraphic(rootContent);
        }
    }
}