package com.scheduler;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SimulatorUI ui = new SimulatorUI(primaryStage);
        ui.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}