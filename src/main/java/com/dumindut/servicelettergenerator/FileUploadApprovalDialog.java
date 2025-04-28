package com.dumindut.servicelettergenerator;

import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import java.util.Optional;
import javafx.scene.control.*;


public class FileUploadApprovalDialog {

    private String actionInitiatedBy;
    private String actionApprovedBy;
    private String comment;

    public boolean showAndWait() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Approval Confirmation");
        dialog.setHeaderText("Please provide approval details for Excel file upload:");

        ButtonType approveButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(approveButtonType, ButtonType.CANCEL);

        TextField initiatedByField = new TextField();
        TextField approvedByField = new TextField();
        TextArea commentArea = new TextArea();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Action Initiated By:"), 0, 0);
        grid.add(initiatedByField, 1, 0);
        grid.add(new Label("Action Approved By:"), 0, 1);
        grid.add(approvedByField, 1, 1);
        grid.add(new Label("Comment:"), 0, 2);
        grid.add(commentArea, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Disable confirm button until all fields are filled
        Node confirmButton = dialog.getDialogPane().lookupButton(approveButtonType);
        confirmButton.setDisable(true);

        initiatedByField.textProperty().addListener((observable, oldValue, newValue) -> validateFields(initiatedByField, approvedByField, commentArea, confirmButton));
        approvedByField.textProperty().addListener((observable, oldValue, newValue) -> validateFields(initiatedByField, approvedByField, commentArea, confirmButton));
        commentArea.textProperty().addListener((observable, oldValue, newValue) -> validateFields(initiatedByField, approvedByField, commentArea, confirmButton));

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == approveButtonType) {
            actionInitiatedBy = initiatedByField.getText();
            actionApprovedBy = approvedByField.getText();
            comment = commentArea.getText();
            return true;
        } else {
            return false;
        }
    }

    private void validateFields(TextField initiatedBy, TextField approvedBy, TextArea commentArea, Node confirmButton) {
        boolean disable = initiatedBy.getText().trim().isEmpty() ||
                approvedBy.getText().trim().isEmpty() ||
                commentArea.getText().trim().isEmpty();
        confirmButton.setDisable(disable);
    }

    public String getActionInitiatedBy() {
        return actionInitiatedBy;
    }

    public String getActionApprovedBy() {
        return actionApprovedBy;
    }

    public String getComment() {
        return comment;
    }
}

