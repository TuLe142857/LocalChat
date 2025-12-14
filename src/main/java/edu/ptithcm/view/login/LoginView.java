package edu.ptithcm.view.login;

import edu.ptithcm.model.Credential;
import edu.ptithcm.security.CredentialManager;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.service.AuthService;
import edu.ptithcm.view.LoginCallback;
import edu.ptithcm.view.base.BaseView;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.KeyPair;
import java.util.*;

public class LoginView extends BaseView {
    private final LoginCallback onLogin;


    private Credential defaultCredential;
    private Credential choosenCredential;
    private ObservableMap<String, InetAddress> availableNetwork;
    private ObservableList<InetAddress> availableIp;

    private TextField userNameTextFied;
    private TextField userIdTextField;
    private ComboBox<InetAddress> ipComboBox;
    private TextField portTextField;
    private Button loadDefaultCredentialButton;
    private Button loginButton;

    public LoginView(LoginCallback onLogin) {
        this.onLogin = onLogin;
    }

    @Override
    protected void init() {
        availableNetwork = FXCollections.observableHashMap();
        availableIp = FXCollections.observableArrayList();
        defaultCredential = CredentialManager.readStoredCredential();
        choosenCredential = defaultCredential;
    }

    @Override
    protected void setupUI() {

        // Main VBox (Wrapper) for centering content
        VBox centerBox = new VBox(25);
        centerBox.setMaxWidth(350); // Constrain width for better look
        centerBox.setPadding(new Insets(40));
        centerBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Title
        Label loginLabel = new Label("LocalChat P2P Login");
        loginLabel.setStyle("-fx-font-size: 2em; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        VBox.setMargin(loginLabel, new Insets(0, 0, 10, 0));
        centerBox.setAlignment(Pos.CENTER);


        // Form (GridPane for alignment)
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(15);
        gridPane.setAlignment(Pos.CENTER);

        // component
        userNameTextFied = new TextField();
        userNameTextFied.setPromptText("Enter your name");
        userNameTextFied.setStyle("-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

        userIdTextField = new TextField();
        userIdTextField.setEditable(false);
        userIdTextField.setStyle("-fx-background-color: #ecf0f1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

        ipComboBox = new ComboBox<>(availableIp);
        ipComboBox.setMaxWidth(Double.MAX_VALUE);
        ipComboBox.setStyle("-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5;");


        portTextField = new TextField();
        portTextField.setText("9999");
        portTextField.setStyle("-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

        Button randomCredentialButton = new Button("Generate New ID");
        randomCredentialButton.setMaxWidth(Double.MAX_VALUE);
        randomCredentialButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10;");


        loadDefaultCredentialButton = new Button("Load Saved ID");
        loadDefaultCredentialButton.setMaxWidth(Double.MAX_VALUE);
        loadDefaultCredentialButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10;");
        if(defaultCredential == null)
            loadDefaultCredentialButton.setDisable(true);

        loginButton = new Button("Login and Connect");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 1.2em; -fx-background-radius: 5; -fx-padding: 12 0;");


        // setup component to layout
        gridPane.add(new Label("User Name"), 0, 0);
        gridPane.add(userNameTextFied, 1, 0);

        gridPane.add(new Label("User ID (Hash)"), 0, 1);
        gridPane.add(userIdTextField, 1, 1);

        gridPane.add(new Label("Network IP"), 0, 2);
        gridPane.add(ipComboBox, 1, 2);

        gridPane.add(new Label("Port"), 0, 3);
        gridPane.add(portTextField, 1, 3);

        // Buttons
        HBox credentialButtons = new HBox(10, randomCredentialButton, loadDefaultCredentialButton);
        credentialButtons.setAlignment(Pos.CENTER);
        credentialButtons.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(randomCredentialButton, Priority.ALWAYS);
        HBox.setHgrow(loadDefaultCredentialButton, Priority.ALWAYS);

        // Combining title and form
        centerBox.getChildren().addAll(loginLabel, gridPane, credentialButtons, loginButton);

        // Add centerBox to the view
        this.setStyle("-fx-background-color: #ecf0f1;"); // Light background for the whole stage
        this.getChildren().add(centerBox);
        StackPane.setAlignment(centerBox, Pos.CENTER);

        // Action listeners (Keep original logic)
        loginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Credential credential = new Credential(choosenCredential.getPublicKey(), choosenCredential.getPrivateKey(), userNameTextFied.getText());
                onLogin.onLogin(credential, ipComboBox.getValue(), Integer.parseInt(portTextField.getText()));
            }
        });

        loadDefaultCredentialButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(defaultCredential != null){
                    userNameTextFied.setText(defaultCredential.getName());
                    userIdTextField.setText(defaultCredential.getId());
                    choosenCredential = defaultCredential;
                }
            }
        });

        randomCredentialButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                KeyPair kp = CryptoUtils.generateRSAKeyPair();
                choosenCredential = new Credential(kp, "");
                userNameTextFied.setText(choosenCredential.getName());
                userIdTextField.setText(choosenCredential.getId());
            }
        });
    }

    @Override
    public void loadData() {
        defaultCredential = CredentialManager.readStoredCredential();
        availableNetwork = FXCollections.observableHashMap();
        availableIp.clear(); // Clear the list before re-adding

        // Load network interface (Keep original logic)
        try{
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while(nets.hasMoreElements()){
                NetworkInterface ni = nets.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual())
                    continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {

                        // Chỉ lấy địa chỉ private LAN
                        String ip = addr.getHostAddress();

                        if (ip.startsWith("192.168.") ||
                                ip.startsWith("10.") ||
                                ip.startsWith("172."))
                        {
                            availableNetwork.put(ni.getDisplayName(), addr);
                            availableIp.add(addr);
                        }
                    }
                }
            }
            // IO.println("On login view, scan for available LAN network:"); // Removed IO.println from UI code for cleaner output
            for(var ip:availableNetwork.entrySet()){
                // IO.println(ip.getKey() + ": " + ip.getValue()); // Removed
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        // Set default values after load
        if (choosenCredential != null) {
            userNameTextFied.setText(choosenCredential.getName());
            userIdTextField.setText(choosenCredential.getId());
        } else {
            // Automatically generate a random ID if no default is found
            KeyPair kp = CryptoUtils.generateRSAKeyPair();
            choosenCredential = new Credential(kp, "");
            userNameTextFied.setText(choosenCredential.getName());
            userIdTextField.setText(choosenCredential.getId());
            loadDefaultCredentialButton.setDisable(true);
        }

        if (!availableIp.isEmpty()) {
            ipComboBox.getSelectionModel().selectFirst();
        } else {
            // Fallback for no LAN IP found
            ipComboBox.setPromptText("No LAN IP found");
        }
    }

    @Override
    public void setupEventBus() {
    }
}