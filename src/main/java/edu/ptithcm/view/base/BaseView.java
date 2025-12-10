package edu.ptithcm.view.base;

import javafx.scene.layout.StackPane;

public abstract class BaseView extends StackPane implements IViewController {

    public BaseView() {
        init();
        setupUI();
        loadData();
        setupEventBus();
    }

    protected abstract void init();
    protected abstract void setupUI();

    @Override
    public void onRemove() {
        System.out.println("Cleaning up view: " + this.getClass().getSimpleName());
    }
}