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
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import org.w3c.dom.Text;

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

        //layout
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(15);
        gridPane.setPadding(new Insets(20, 20, 20, 20));
        gridPane.setAlignment(Pos.CENTER);

        // component
        TextField userNameTextFied = new TextField();

        TextField userIdTextField = new TextField();
        userIdTextField.setEditable(false);

        ComboBox<InetAddress> ipComboBox = new ComboBox<>(availableIp);

        TextField portTextField = new TextField();
        portTextField.setText("9999");

        Button randomCredentialButton = new Button("Random credential");
        randomCredentialButton.setMaxWidth(Double.MAX_VALUE);



        Button loadDefaultCredentialButton = new Button("Get Default Credential");
        loadDefaultCredentialButton.setMaxWidth(Double.MAX_VALUE);
        if(defaultCredential == null)
            loadDefaultCredentialButton.setDisable(true);

        Button loginButton = new Button("login");
        loginButton.setMaxWidth(Double.MAX_VALUE);


        //action listener
        loginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
//                AuthService.login(
//                        new Credential(choosenCredential.getPublicKey(), choosenCredential.getPrivateKey(), userNameTextFied.getText()),
//                        ipComboBox.getValue(),
//                        Integer.parseInt(portTextField.getText())
//                );
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

        // setup component to layout
        Label loginLabel = new Label("Login");
        loginLabel.setAlignment(Pos.CENTER);
        loginLabel.setMaxWidth(Double.MAX_VALUE);
        gridPane.add(loginLabel, 0, 0, 2, 1);
        GridPane.setHalignment(loginLabel, HPos.CENTER);

        gridPane.add(new Label("User name"), 0, 1);
        gridPane.add(userNameTextFied, 1, 1);

        gridPane.add(new Label("User id"), 0, 2);
        gridPane.add(userIdTextField, 1, 2);

        gridPane.add(new Label("Network"), 0, 3);
        gridPane.add(ipComboBox, 1, 3);

        gridPane.add(new Label("Port"), 0, 4);
        gridPane.add(portTextField, 1, 4);

        gridPane.add(randomCredentialButton, 0, 5, 2, 1);
        GridPane.setHalignment(randomCredentialButton, HPos.CENTER);

        gridPane.add(loadDefaultCredentialButton, 0, 6, 2, 1);
        GridPane.setHalignment(loadDefaultCredentialButton, HPos.CENTER);

        gridPane.add(loginButton, 0, 7, 2, 1);
        GridPane.setHalignment(loginButton, HPos.CENTER);

        this.getChildren().add(gridPane);
        StackPane.setAlignment(gridPane, Pos.CENTER);
    }

    @Override
    public void loadData() {
        defaultCredential = CredentialManager.readStoredCredential();
        availableNetwork = FXCollections.observableHashMap();
        //load network interface
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
            IO.println("On login view, scan for available LAN network:");
            for(var ip:availableNetwork.entrySet()){
                IO.println(ip.getKey() + ": " + ip.getValue());
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void setupEventBus() {
    }
}
