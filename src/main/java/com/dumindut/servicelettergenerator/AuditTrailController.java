package com.dumindut.servicelettergenerator;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AuditTrailController {

    @FXML
    private TableView<AuditTrailRecord> auditTable;

    @FXML
    private TableColumn<AuditTrailRecord, String> colActionedTime;
    @FXML
    private TableColumn<AuditTrailRecord, String> colAction;
    @FXML
    private TableColumn<AuditTrailRecord, String> colDescription;
    @FXML
    private TableColumn<AuditTrailRecord, String> colComment;
    @FXML
    private TableColumn<AuditTrailRecord, String> colInitiatedBy;
    @FXML
    private TableColumn<AuditTrailRecord, String> colApprovedBy;

    private final DatabaseHandler dbHandler = new SQLiteDatabaseHandler();

    @FXML
    private void initialize() {
        colActionedTime.setCellValueFactory(cellData -> cellData.getValue().actionedTimeProperty());
        colAction.setCellValueFactory(cellData -> cellData.getValue().actionProperty());
        colDescription.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        colComment.setCellValueFactory(cellData -> cellData.getValue().userCommentProperty());
        colInitiatedBy.setCellValueFactory(cellData -> cellData.getValue().initiatedByProperty());
        colApprovedBy.setCellValueFactory(cellData -> cellData.getValue().approvedByProperty());

        loadAuditLogs();
    }

    private void loadAuditLogs() {
        ObservableList<AuditTrailRecord> logs = dbHandler.getAllAuditLogs();
        auditTable.setItems(logs);
    }
}
