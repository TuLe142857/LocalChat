package edu.ptithcm.view;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.Credential;
import edu.ptithcm.view.base.IViewController;
import edu.ptithcm.view.login.LoginView;
import edu.ptithcm.view.main.MainLayout;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ViewManager {
    private final Stage primaryStage;
    private IViewController currentViewController;
    private final LoginCallback loginCallback;
    private final LogoutCallback logoutCallback;
    private final ExitCallback exitCallback;

    public ViewManager(Stage primaryStage, LoginCallback onLogin, LogoutCallback onLogout, ExitCallback onExit){
        this.primaryStage = primaryStage;
        this.loginCallback = onLogin;
        this.logoutCallback = onLogout;
        this.exitCallback = onExit;

        primaryStage.setTitle("Local Chat P2P - bình tĩnh, chưa code app GUI");

        // width, height, position
        Screen screen = Screen.getPrimary();
        double windowDefaultWidth = screen.getBounds().getWidth()*2/3;
        double windowDefaultHeight = screen.getBounds().getHeight()*2/3;
        primaryStage.setWidth(windowDefaultWidth);
        primaryStage.setHeight(windowDefaultHeight);
        primaryStage.centerOnScreen();
        primaryStage.maximizedProperty().addListener((obs, wasMax, isMax) -> {
            if (isMax) {
                IO.println("Window maximized");
            } else {
                primaryStage.setWidth(windowDefaultWidth);
                primaryStage.setHeight(windowDefaultHeight);
                primaryStage.centerOnScreen();
                IO.println("Window restored");
            }
        });

        // exit
        primaryStage.setOnCloseRequest(event->{this.exitCallback.onExit();});

        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public void showLoginView(){
        if(currentViewController != null)
            currentViewController.onRemove();

        Scene scene = new Scene(new LoginView(this.loginCallback));
        primaryStage.setScene(scene);
    }

    public void showMainView(){
        this.primaryStage.setTitle("App chat P2P - "+Cache.getInstance().getCredential().getName());
        if(currentViewController != null)
            currentViewController.onRemove();

        Scene scene = new Scene(new MainLayout(this.logoutCallback::onLogout));
        primaryStage.setScene(scene);
    }
}
