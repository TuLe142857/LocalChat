package edu.ptithcm;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.network.NetworkService;
import edu.ptithcm.network.core.ConnectionPool;
import edu.ptithcm.service.AuthService;
import edu.ptithcm.view.ViewManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application{
    private NetworkService networkService;
    private ViewManager viewManager;
    @Override
    public void init() throws Exception {
        super.init();

    }

    @Override
    public void start(Stage stage) throws Exception {
        this.viewManager = new ViewManager(
                stage,

                // on login action
                // - write credential to cache
                // - create & start network service
                ((credential, ip, port) -> {
                    AuthService.login(credential, ip, port);
                    this.networkService = new NetworkService(ip, port);
                    this.networkService.start();
                    this.viewManager.showMainView();
                }),

                // on logout action
                // - clear cache
                // - stop network service
                // - remove all tcp connection
                (()->{
                    this.networkService.stop();
                    Cache.getInstance().clear();
                    ConnectionPool.getInstance().clear();
                    this.viewManager.showLoginView();
                }),

                // on exit action
                ()->{
                    if(this.networkService != null)
                        this.networkService.stop();
                    Cache.getInstance().clear();
                    ConnectionPool.getInstance().clear();
                }
        );
        viewManager.showLoginView();
    }
}
