package com.example.mindmap;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.view.MainFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MindMapApplication extends Application {
    @Override
    public void start(Stage stage) {
        MindMapController controller = new MindMapController(stage);
        MainFrame frame = controller.getMainFrame();
        Scene scene = new Scene(frame, 1280, 780);
        scene.getStylesheets().add(MindMapApplication.class.getResource("/com/example/mindmap/styles.css").toExternalForm());
        controller.installShortcuts(scene);
        stage.setMinWidth(1040);
        stage.setMinHeight(660);
        stage.setScene(scene);
        controller.newMapSilently();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
