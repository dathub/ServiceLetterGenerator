package com.dumindut.servicelettergenerator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
            Parent root = loader.load();

            // Get Screen Dimensions
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Set Scene Size to 90% of Screen Size
            double width = screenBounds.getWidth() * 0.9;
            double height = screenBounds.getHeight() * 0.9;

            Scene scene = new Scene(root, width, height);

            primaryStage.setTitle("Service Letter Generator");
            Image mainIcon = new Image(MainApp.class.getResourceAsStream("/images/AppLogo.png"));
            primaryStage.getIcons().add(mainIcon);
            primaryStage.setScene(scene);

            // Maximize the Window (Optional)
            primaryStage.setMaximized(true);

            primaryStage.show();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

