package edu.ptithcm.view.main.search;

import edu.ptithcm.model.Peer;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class PeerListItem extends ListCell<Peer> {
    private final VBox content;
    private final Label nameLabel;
    private final Label detailLabel;

    public PeerListItem() {
        super();
        nameLabel = new Label();
        nameLabel.setStyle("-fx-font-weight: bold;");

        detailLabel = new Label();
        detailLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        content = new VBox(nameLabel, detailLabel);
        content.setSpacing(2);
        content.setPadding(new Insets(5, 10, 5, 10));
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
            setGraphic(content);
        }
    }
}