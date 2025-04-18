package com.dumindut.servicelettergenerator;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;

public class ErrorView {

    public static void showErrors(List<String> errors, String title, String errorLabelTxt) {
        Stage stage = new Stage();
        stage.setTitle(title);
        Image errorIcon = new Image(ErrorView.class.getResourceAsStream("/images/error-10376.png"));
        stage.getIcons().add(errorIcon);

        Label errorLabel = new Label(errorLabelTxt);
        ListView<String> errorListView = new ListView<>();
        errorListView.getItems().addAll(errors);

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());

        VBox layout = new VBox(10, errorLabel, errorListView, closeButton);
        Scene scene = new Scene(layout, 500, 400);

        stage.setScene(scene);
        stage.show();
    }
}