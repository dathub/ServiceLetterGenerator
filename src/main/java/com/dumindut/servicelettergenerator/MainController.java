package com.dumindut.servicelettergenerator;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.geometry.Insets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static com.dumindut.servicelettergenerator.Defs.*;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    @FXML
    private Button btnUpload;
    @FXML
    private Button btnGenerate;
    @FXML
    private Button btnGenerateAll;
    @FXML
    private Button btnCleanDB;
    @FXML
    private Button btnFilter;
    @FXML
    private Button btnClearFilter;
//    @FXML
//    private Button btnUploadToDrive;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtMembershipNo;
    @FXML
    private TextField txtProjectPeriod;
    @FXML
    private TableView<FileRecord> tableView;
    @FXML
    private TableColumn<FileRecord, String> nameCol;
    @FXML
    private TableColumn<FileRecord, String> membershipNoCol;
    @FXML
    private TableColumn<FileRecord, String> projectCol;
    @FXML
    private TableColumn<FileRecord, String> projectCodeCol;
    @FXML
    private TableColumn<FileRecord, String> projectDateCol;
    @FXML
    private TableColumn<FileRecord, String> subCommitteeCol;
    @FXML
    private TableColumn<FileRecord, String> projectPeriodCol;
    @FXML
    private TableColumn<FileRecord, String> lastUpdatedTimeColumn;
    @FXML
    private TableColumn<FileRecord, String> approvedByColumn;
    @FXML
    private TableColumn<FileRecord, String> documentIdCol;
    @FXML
    private TableColumn<FileRecord, String> documentDateCol;
    @FXML
    private TableColumn<FileRecord, String> dbKey;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;
    @FXML
    private Label lblRowCount;


    private ObservableList<FileRecord> masterData;
    private FilteredList<FileRecord> filteredData;
    private final DatabaseHandler dbHandler = new SQLiteDatabaseHandler();
    private final Validator validator = new Validator();
    private final ExcelProcessor excelProcessor = new ExcelProcessor();
    private DocumentGenerator wordToPDFGenerator = new DocumentGenerator();
    private List<String> documentGenerationErrors = new ArrayList<>();

    @FXML
    public void initialize() {
        progressIndicator.setVisible(false);
        statusLabel.setVisible(false);
        btnUpload.setOnAction(e -> handleUpload());
        btnGenerate.setOnAction(e -> handleDocumentGenerate());
        btnGenerateAll.setOnAction(e -> handleGenerateAllDocuments());
        btnCleanDB.setOnAction(e -> handleCleanDB());
        btnFilter.setOnAction(e -> applyFilter());
//        btnUploadToDrive.setOnAction(e -> handleUploadToDrive());
        btnClearFilter.setOnAction(e -> applyClearFilter());
        setupTableView();
        loadTableData();
        setupTableViewCellContextMenu();
    }

    @FXML
    private void handleClose() {
        Platform.exit();
    }

    @FXML
    private void handleIssueTroubleshooting() {
        // Replace with your logic, e.g., show a dialog or open a web page
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Issue Troubleshooting");
        alert.setHeaderText(null);
        alert.setContentText("For application issues, please contact < Main developer - J.G.D.A.Thilakaratne (2010 Batch), Whatsapp 0716319494 >");
        alert.showAndWait();
    }

    @FXML
    private void handleUserManual() {
        // Replace with your logic, e.g., show manual or link
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Manual");
        alert.setHeaderText(null);
        alert.setContentText("User manual content or download link goes here...");
        alert.showAndWait();
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(null);
        alert.setContentText("This application was developed to ease the Service Letter issuing process. Earlier, " +
                "a separate Service Letters were issued for each member contribution. This application will provide a " +
                "consolidated service letter for all member contributions.");
        alert.showAndWait();
    }

    private void handleUpload() {
        logger.debug("Upload button clicked");

        FileUploadApprovalDialog approvalDialog = new FileUploadApprovalDialog();
        boolean approved = approvalDialog.showAndWait();

        if(!approved){
            logger.debug("Upload not approved");
            return;
        }


        String initiatedBy = approvalDialog.getActionInitiatedBy();
        String approvedBy = approvalDialog.getActionApprovedBy();
        String comment = approvalDialog.getComment();
        String finalApprovalTxt = getFinalApprovalTxt(initiatedBy, approvedBy);

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xls", "*.xlsx"));
        fileChooser.setTitle("Select Excel File");

        File file = fileChooser.showOpenDialog(new Stage());

        if (file == null) {
            logger.info("File selection cancelled.");
            return; // or return false, or exit the method early
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return processExcelFile(file, finalApprovalTxt);
            }
        };

        task.setOnRunning(e -> {
            progressIndicator.setVisible(true);
            statusLabel.setVisible(true);
            statusLabel.setText("Uploading and processing Excel file...");
        });

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            statusLabel.setVisible(false);
            setTableRowCountValue();
            boolean success = task.getValue();
            if (success) {
                dbHandler.logAuditTrail("FILE UPLOAD", file.getAbsolutePath() + " uploaded.", comment, initiatedBy, approvedBy);
                showNotificationAlert("Excel file processed successfully.", Alert.AlertType.INFORMATION);
            } else {
                String title = "Excel File Validation Errors";
                String errorLabelTxt = "The following errors were found in the uploaded Excel file:";
                ErrorView.showErrors(validator.getErrors(), title, errorLabelTxt); // Show errors in a pop-up
            }
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            statusLabel.setVisible(false);
            Throwable ex = task.getException();
            logger.error("Error processing Excel file", ex);
            showNotificationAlert("Failed to process Excel file: " + ex.getMessage(), Alert.AlertType.ERROR);
        });

        new Thread(task).start();
    }

    private String getFinalApprovalTxt(String initiatedBy, String approvedBy) {
        String finalApprovalTxt = "Actioned by [ " + initiatedBy + " ] and Approved by [ " + approvedBy + " ]";
        return finalApprovalTxt;
    }

    private boolean processExcelFile(File file, String approvalComment) {
        validator.resetErrors();
        if (validator.validateFile(file)) {
            excelProcessor.processExcel(file, dbHandler, approvalComment);

            // Refresh filteredData to reflect new records
            filteredData.setPredicate(null); // Reset predicate to allow new data
            filteredData = new FilteredList<>(dbHandler.getAllRecords(), p -> true);
            tableView.setItems(filteredData);
            return true;
        }
        return false;
    }

    private void handleDocumentGenerate() {
        logger.debug("Generate PDFs clicked");

        ObservableList<FileRecord> dataSource = tableView.getItems();

        //generateSingleDocumentForAllItemsForMember(dataSource);
        File directoryToSave = getSaveLocation();
        if (directoryToSave == null) {
            logger.info("Directory selection cancelled.");
            return; // or exit early
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                documentGenerationErrors.clear();
                return generateDocumentsForMember(dataSource, directoryToSave, null);
            }
        };

        task.setOnRunning(e -> {
            progressIndicator.setVisible(true);
            statusLabel.setVisible(true);
            statusLabel.setText("Generating documents...");
        });

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            statusLabel.setVisible(false);
            boolean success = task.getValue();
            if (success) {
                dbHandler.logAuditTrail("FILE GENERATE", "File generated successfully.", "NA", "NA", "NA");
                showNotificationAlert(INFO_DOC_GEN_SUCCESS, Alert.AlertType.INFORMATION);
            } else {
                ErrorView.showErrors(documentGenerationErrors, "Document Generation Errors", "Following issues encountered when generating the documents");
            }
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            statusLabel.setVisible(false);
            Throwable ex = task.getException();
            logger.error("Error generating documents", ex);
            showNotificationAlert("Failed to generate documents: " + ex.getMessage(), Alert.AlertType.ERROR);
        });

        new Thread(task).start();
    }

    private void generateSingleDocumentForAllItemsForMember(ObservableList<FileRecord> dataSource) {
        if(dataSource.isEmpty()){
            logger.error("No data in the table view");
            showNotificationAlert("No data in the table view.", Alert.AlertType.ERROR);
            return;
        }

        Set<String> singularCheckForMemberName = new HashSet<>();
        Set<String> singularCheckForMemberId = new HashSet<>();
        Set<String> singularCheckForProjectPeriod = new HashSet<>();
        List<List<String>> tableData = new ArrayList<>();
        List<String> headerData = List.of("Folio","Project","Project Date","Sub Committee/Exec. Officer");
        tableData.add(headerData);
        int i=0;
        for (FileRecord fileRecord : dataSource) {
            List<String> row = new ArrayList<>();
            i++;
            row.add(String.valueOf(i));
            row.add(fileRecord.getProject());
            row.add(fileRecord.getProjectDate());
            row.add(fileRecord.getSubCommittee());
            tableData.add(row);

            singularCheckForMemberName.add(fileRecord.getName());
            singularCheckForMemberId.add(fileRecord.getMembershipNo());
            singularCheckForProjectPeriod.add(fileRecord.getProjectPeriod());
        }

        if(singularCheckForMemberName.size() != 1 || singularCheckForMemberId.size() != 1) {
            logger.error("filteredData is not for a single user");
            showNotificationAlert("Table contains information for multiple users. Please filter the data for a single user.", Alert.AlertType.ERROR);
            return;
        }

        if(singularCheckForProjectPeriod.size() != 1) {
            logger.error("filteredData is not for a single project period");
            showNotificationAlert("Table contains information for multiple project periods. Please filter the data for a single project period.", Alert.AlertType.ERROR);
            return;
        }

        File saveDirectory = getSaveLocation();
        if (saveDirectory == null) {
            logger.error("Save directory is null");
            return;
        }

        Map<String, String> replacements = new HashMap<>();
        FileRecord firstRecord = dataSource.getFirst();
        String docId = getDocumentId(firstRecord.getMembershipNo());
        replacements.put(VAR_MEMBER_NAME, firstRecord.getName());
        replacements.put(VAR_MEMBERSHIP_NO, firstRecord.getMembershipNo());
        replacements.put(VAR_NIC, "UNDEFINED");
        replacements.put(VAR_LETTER_NO, docId);
        replacements.put(VAR_LETTER_DATE, generateLetterDate(firstRecord.getMembershipNo()));
        replacements.put(VAR_ROW_COUNT, String.valueOf(i));
        replacements.put(VAR_SERVICE_PERIOD, firstRecord.getProjectPeriod());

        boolean isSuccess = wordToPDFGenerator.generatePdf(replacements, tableData, saveDirectory, docId);

        if(isSuccess) {
            showNotificationAlert("Document generated successfully", Alert.AlertType.INFORMATION);
        } else {
            showNotificationAlert("Error occurred when generating the document", Alert.AlertType.ERROR);
        }
    }

    private boolean generateDocumentsForMember(ObservableList<FileRecord> dataSource, File directoryToSave, String membershipNo) {

        String memberInfoSuffix = ". ";
        if(membershipNo != null){
            memberInfoSuffix = " for Member - " + membershipNo + ". ";
        }

        if(dataSource.isEmpty()){
            String err = "No data to generate document" + memberInfoSuffix;
            logger.error(err);
            documentGenerationErrors.add(err);
            return false;
        }

        Set<String> singularCheckForMemberName = new HashSet<>();
        Set<String> singularCheckForMemberId = new HashSet<>();
        Set<String> singularCheckForProjectPeriod = new HashSet<>();
        List<List<List<String>>> tableDataChunks = new ArrayList<>();
        List<String> chunkDbKeys = new ArrayList<>();
        List<List<String>> tableData = new ArrayList<>();
        ;
        List<String> headerData = List.of("Folio","Project","Project Date","Sub Committee/Exec. Officer");
        tableData.add(headerData);

        int i=0;
        int totalCount=0;
        for (FileRecord fileRecord : dataSource) {

            totalCount++;

            if((i != 0) && (i % MAX_PAGE_ENTRY_COUNT == 0)){
                tableData.add(chunkDbKeys);
                tableDataChunks.add(tableData);
                i=0;
                chunkDbKeys = new ArrayList<>();
                tableData = new ArrayList<>();
                tableData.add(headerData);
            } else {
            }

            List<String> row = new ArrayList<>();
            row.add(String.valueOf(++i));
            row.add(fileRecord.getProject());
            row.add(fileRecord.getProjectDate());
            row.add(fileRecord.getSubCommittee());
            tableData.add(row);

            chunkDbKeys.add(fileRecord.getDbPrimaryKeyId());

            singularCheckForMemberName.add(fileRecord.getName());
            singularCheckForMemberId.add(fileRecord.getMembershipNo());
            singularCheckForProjectPeriod.add(fileRecord.getProjectPeriod());
        }

        if(i <= MAX_PAGE_ENTRY_COUNT && totalCount == dataSource.size()) {
            tableData.add(chunkDbKeys);
            tableDataChunks.add(tableData);
        }

        if(singularCheckForMemberName.size() != 1 || singularCheckForMemberId.size() != 1) {
            String err = "Document generation failed" + memberInfoSuffix + "Data is not for a single user. Please filter the data for a single user.";
            logger.error(err);
            documentGenerationErrors.add(err);
            return false;
        }

        if(singularCheckForProjectPeriod.size() != 1) {
            String err = "Document generation failed" + memberInfoSuffix + "Data is not for a single project period. Please filter the data for a single project period.";
            logger.error(err);
            documentGenerationErrors.add(err);
            return false;
        }

        Map<String, String> replacements = new HashMap<>();
        FileRecord firstRecord = dataSource.getFirst();
        replacements.put(VAR_MEMBER_NAME, firstRecord.getName());
        replacements.put(VAR_MEMBERSHIP_NO, firstRecord.getMembershipNo());
        replacements.put(VAR_NIC, "UNDEFINED");
        replacements.put(VAR_LETTER_DATE, generateLetterDate(firstRecord.getMembershipNo()));
        replacements.put(VAR_SERVICE_PERIOD, firstRecord.getProjectPeriod());

        logger.info("Total documents for the user[" + firstRecord.getMembershipNo() + "|"+ firstRecord.getName() + "] - " + tableDataChunks.size());

        String generatedDocumentIdPrefix = generateDocumentId(firstRecord.getMembershipNo());
        boolean fileGenerationSuccess = true;
        int chunkNo = 0;
        for (List<List<String>> chunk: tableDataChunks) {
            String docId = generatedDocumentIdPrefix + "-L" + (++chunkNo);
            replacements.put(VAR_LETTER_NO, docId);
            chunkDbKeys = chunk.getLast();
            for (String dbKey: chunkDbKeys) {
                boolean success = dbHandler.updateDBDocId(dbKey, docId);
                if(!success){
                    String error = "Document Id DB update failed for User["+ firstRecord.getMembershipNo() + "] - DB key - " + dbKey + "  | Document Id - " + docId;
                    logger.error(error);
                    documentGenerationErrors.add(error);
                    return false;
                }
            }

            refreshTableData();

            chunk.removeLast(); //removing unwanted last row which has the DBKeys
            replacements.put(VAR_ROW_COUNT, String.valueOf(chunk.size() - 1)); //Remove the header row from row count

            boolean success = wordToPDFGenerator.generatePdf(replacements, chunk, directoryToSave, docId);
            fileGenerationSuccess = fileGenerationSuccess && success;
        }

        return fileGenerationSuccess;
    }

    private File getSaveLocation() {
        // Set up the directory chooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Save Files");

        // Show directory chooser dialog
        File selectedDirectory = directoryChooser.showDialog(new Stage());
        return selectedDirectory;
    }

    public String getDocumentId(String membershipNo) {
        String documentId = dbHandler.getExistingDocumentId(membershipNo);

        if (documentId == null || documentId.trim().isEmpty()) {
            documentId = generateDocumentId(membershipNo);
            boolean success = dbHandler.updateNewDocumentId(membershipNo, documentId);// Generate a new one
            if(success) {
                refreshTableData();
            } else {
                logger.error("Error occurred during updating document_id in DB.");
            }
        }

        return documentId != null ? documentId : "";
    }

    private String generateDocumentId(String membershipNo) {

        String salt = "DAT";
        String combined = membershipNo + salt;
        int hash = 0;
        for (char c:combined.toCharArray()) {
            hash += (int)c;
        }
        hash = hash % 100000;

        return membershipNo + "-" + hash;
    }

    private String generateLetterDate(String membershipNo) {
        String documentDate = dbHandler.getExistingDocumentDate(membershipNo);

        if (documentDate == null || documentDate.trim().isEmpty()) {
            documentDate = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
            boolean success = dbHandler.updateNewDocumentDate(membershipNo, documentDate);
            if(success){
                refreshTableData();
            } else {
                logger.error("Error occurred during updating document_date in DB.");
            }
        }
        return documentDate != null ? documentDate : "";
    }

    private void handleGenerateAllDocuments() {

        //Ask for project period input
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Project Period Required");
        dialog.setHeaderText("Enter the Project Period (e.g., 2022-2023)");
        dialog.setContentText("Project Period:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            logger.info("Project period input cancelled.");
            return;
        }
        String enteredProjectPeriod = result.get().trim();
        if (enteredProjectPeriod.isEmpty()) {
            showNotificationAlert("Project Period cannot be empty!", Alert.AlertType.WARNING);
            return;
        }

        if (!enteredProjectPeriod.matches("\\d{4}-\\d{4}")) {
            showNotificationAlert("Invalid Project Period format! Please use format: yyyy-yyyy (e.g., 2022-2023)", Alert.AlertType.ERROR);
            return;
        }


        //Get all unique membership nos from the DB.
        List<String> membershipIds = dbHandler.getUniqueMembershipNos();
        File directoryToSave = getSaveLocation();
        if (directoryToSave == null) {
            logger.info("Directory selection cancelled.");
            return; // or exit early
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                documentGenerationErrors.clear();
                boolean success = true;

                if (membershipIds.isEmpty()) {
                    documentGenerationErrors.add("No membership numbers found in DB. Please check whether data is available in the table view.");
                    success = false;
                } else {
                    for (String membershipNo : membershipIds) {
                        ObservableList<FileRecord> datasource = dbHandler.getRecordsByMembershipNo(membershipNo);

                        ObservableList<FileRecord> filteredDatasource = datasource.filtered(
                                record -> enteredProjectPeriod.equalsIgnoreCase(record.getProjectPeriod())
                        );

                        boolean ok = generateDocumentsForMember(filteredDatasource, directoryToSave, membershipNo);
                        success = success && ok;
                    }
                }
                return success;
            }
        };

        task.setOnRunning(e -> {
            progressIndicator.setVisible(true);
            statusLabel.setVisible(true);
            statusLabel.setText("Generating all documents...");
        });

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            statusLabel.setVisible(false);
            boolean success = task.getValue();
            if (success) {
                dbHandler.logAuditTrail("MULTIPLE FILES GENERATE", "Files generated successfully.", "NA", "NA", "NA");
                showNotificationAlert(INFO_DOC_GEN_SUCCESS, Alert.AlertType.INFORMATION);
            } else {
                ErrorView.showErrors(documentGenerationErrors, "Document Generation Errors", "Following issues encountered when generating the documents");
            }
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            statusLabel.setVisible(false);
            Throwable ex = task.getException();
            logger.error("Error generating all documents", ex);
            showNotificationAlert("Failed to generate documents: " + ex.getMessage(), Alert.AlertType.ERROR);
        });

        new Thread(task).start();
    }

    private void handleCleanDB() {
        logger.debug("Clean Database clicked");
        if (showConfirmationDialog()) {
            // Proceed to clean database
            if(dbHandler.cleanDatabase()) {
                refreshTableData();
                showNotificationAlert("The database has been successfully cleaned.", Alert.AlertType.INFORMATION);
            } else {
                showNotificationAlert(ERROR_SYSTEM, Alert.AlertType.ERROR);
            }
        }
    }

    private void setupTableView() {
        // Define columns
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        membershipNoCol.setCellValueFactory(cellData -> cellData.getValue().membershipNoProperty());
        projectCol.setCellValueFactory(cellData -> cellData.getValue().projectProperty());
        projectCodeCol.setCellValueFactory(cellData -> cellData.getValue().projectCodeProperty());
        projectDateCol.setCellValueFactory(cellData -> cellData.getValue().projectDateProperty());
        subCommitteeCol.setCellValueFactory(cellData -> cellData.getValue().subCommitteeProperty());
        projectPeriodCol.setCellValueFactory(cellData -> cellData.getValue().projectPeriodProperty());
        documentIdCol.setCellValueFactory(cellData -> cellData.getValue().documentIdProperty());
        documentDateCol.setCellValueFactory(cellData -> cellData.getValue().documentDateProperty());
        dbKey.setCellValueFactory(cellData -> cellData.getValue().dbPrimaryKeyIdProperty());
        lastUpdatedTimeColumn.setCellValueFactory(cellData -> cellData.getValue().lastUpdatedTimeProperty());
        approvedByColumn.setCellValueFactory(cellData -> cellData.getValue().approvedByProperty());
    }

    public void loadTableData() {
        masterData = FXCollections.observableArrayList(dbHandler.getAllRecords());
        filteredData = new FilteredList<>(masterData, p -> true);
        tableView.setItems(filteredData);
        setTableRowCountValue();
    }

    private void applyFilter() {
        String nameFilter = txtName.getText().trim().toLowerCase();
        String membershipNoFilter = txtMembershipNo.getText().trim().toLowerCase();
        String projectPeriodFilter = txtProjectPeriod.getText().trim().toLowerCase();

        filteredData.setPredicate(record -> {
            boolean nameMatches = nameFilter.isEmpty() || record.getName().toLowerCase().contains(nameFilter);
            boolean membershipMatches = membershipNoFilter.isEmpty() || record.getMembershipNo().toLowerCase().contains(membershipNoFilter);
            boolean projectPeriodMatches = projectPeriodFilter.isEmpty() || record.getProjectPeriod().toLowerCase().contains(projectPeriodFilter);

            return nameMatches && membershipMatches && projectPeriodMatches;
        });

        setTableRowCountValue();

        logger.debug("Filter applied: " + nameFilter + " | " + membershipNoFilter + " | " + projectPeriodFilter);
    }

    private void setTableRowCountValue() {
        if(filteredData != null) {
            lblRowCount.setText(String.valueOf(filteredData.size()));
        }
    }

    private void setupTableViewCellContextMenu() {
        for (TableColumn<FileRecord, ?> column : tableView.getColumns()) {
            setupContextMenuForColumn(column);
        }
    }

    private <T> void setupContextMenuForColumn(TableColumn<FileRecord, T> column) {
        column.setCellFactory(col -> {
            TableCell<FileRecord, T> cell = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null ? null : item.toString()));
                }
            };

            // Create context menu
            ContextMenu contextMenu = new ContextMenu();

            // Copy menu item
            MenuItem copyItem = new MenuItem("Copy cell value");
            copyItem.setOnAction(e -> {
                String cellValue = cell.getText();
                if (cellValue != null) {
                    copyToClipboard(cellValue);
                }
            });

            // Edit menu item
            MenuItem editItem = new MenuItem("Edit record");
            editItem.setOnAction(e -> {
                FileRecord selectedRecord = tableView.getSelectionModel().getSelectedItem();
                if (selectedRecord != null) {
                    openEditDialog(selectedRecord); // You will implement this
                }
            });

            // Delete menu item
            MenuItem deleteItem = new MenuItem("Delete record");
            deleteItem.setOnAction(e -> {
                FileRecord selectedRecord = tableView.getSelectionModel().getSelectedItem();
                if (selectedRecord != null) {
                    deleteRecord(selectedRecord); // You will implement this
                }
            });

            contextMenu.getItems().addAll(copyItem, new SeparatorMenuItem(), editItem, deleteItem);

            // Set context menu only if the cell is not empty
            cell.setOnContextMenuRequested(event -> {
                if (!cell.isEmpty()) {
                    cell.setContextMenu(contextMenu);
                }
            });

            return cell;
        });
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        logger.debug("Copied to clipboard: " + text);
    }


    private void showNotificationAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        String title = "Information";
        if(alertType.equals(Alert.AlertType.ERROR)){
            title = "Error";
        } else if(alertType.equals(Alert.AlertType.WARNING)){
            title = "Warning";
        }
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private <T> void runTask(Task<T> task, String startMessage) {
        progressIndicator.setVisible(true);
        statusLabel.setVisible(true);
        statusLabel.setText(startMessage);
        progressIndicator.progressProperty().bind(task.progressProperty());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private boolean showConfirmationDialog() {
        // Create dialog
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Confirm Action");
        dialog.setHeaderText("This action will delete all the Database records. Please enter credentials to proceed.");

        // Set the button types
        ButtonType loginButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create username and password labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert result to username-password pair
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            String enteredUsername = result.get().getKey();
            String enteredPassword = result.get().getValue();
            // You can verify these against stored credentials
            boolean success = verifyCredentials(enteredUsername, enteredPassword);
            if(success) {
                return true;
            } else {
                showNotificationAlert("Invalid credentials. Database not cleaned.", Alert.AlertType.WARNING);
                return false;
            }
        }

        return false;
    }

    private boolean verifyCredentials(String username, String password) {
        final String VALID_USERNAME = "admin";
        final String VALID_PASSWORD = "oba123";
        return VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
    }

    public void refreshTableData() {
        List<FileRecord> updatedRecords = dbHandler.getAllRecords();
        masterData.setAll(updatedRecords);
        filteredData = new FilteredList<>(masterData, p -> true);
        tableView.setItems(filteredData);
        // Force re-evaluation of the filter
        applyFilter();
    }


    private void applyClearFilter() {
        txtName.clear();
        txtMembershipNo.clear();
        txtProjectPeriod.clear();
        refreshTableData();
    }

    private void openEditDialog(FileRecord fileRecord) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Record");
        dialog.setHeaderText("Modify the selected record:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(fileRecord.getName());
        TextField membershipNoField = new TextField(fileRecord.getMembershipNo());
        TextField projectField = new TextField(fileRecord.getProject());
        TextField projectDateField = new TextField(fileRecord.getProjectDate());
        TextField subCommitteeField = new TextField(fileRecord.getSubCommittee());
        TextField projectPeriodField = new TextField(fileRecord.getProjectPeriod());

        grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Membership No:"), 0, 1); grid.add(membershipNoField, 1, 1);
        grid.add(new Label("Project:"), 0, 2); grid.add(projectField, 1, 2);
        grid.add(new Label("Project Date:"), 0, 3); grid.add(projectDateField, 1, 3);
        grid.add(new Label("Sub Committee:"), 0, 4); grid.add(subCommitteeField, 1, 4);
        grid.add(new Label("Project Period:"), 0, 5); grid.add(projectPeriodField, 1, 5);

        Separator separator = new Separator();
        grid.add(separator, 0, 6, 2, 1);

        TextField initiatedByField = new TextField();
        TextField approverField = new TextField();
        TextArea commentArea = new TextArea();
        commentArea.setPrefRowCount(3);

        grid.add(new Label("Initiated By:"), 0, 7); grid.add(initiatedByField, 1, 7);
        grid.add(new Label("Approved By:"), 0, 8); grid.add(approverField, 1, 8);
        grid.add(new Label("Comment:"), 0, 9); grid.add(commentArea, 1, 9);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (initiatedByField.getText().isBlank() || approverField.getText().isBlank() || commentArea.getText().isBlank()) {
                showNotificationAlert("All approval fields must be filled.", Alert.AlertType.WARNING);
                return;
            }

            String approvalTxt = getFinalApprovalTxt(initiatedByField.getText().trim(), approverField.getText().trim());

            String projectTxt = projectField.getText().trim();
            if(projectTxt.length() > 50){
                showNotificationAlert("Project value exceeds 50 characters", Alert.AlertType.ERROR);
            }

            FileRecord newRecord = new FileRecord(
                    nameField.getText().trim(),
                    membershipNoField.getText().trim(),
                    projectTxt,
                    "",
                    projectDateField.getText().trim(),
                    subCommitteeField.getText().trim(),
                    "",
                    "",
                    projectPeriodField.getText().trim(),
                    "",
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    approvalTxt);

            if (dbHandler.isDuplicate(newRecord)) {
                logger.debug(String.format("Duplicate record found: %s - %s - %s - %s - %s - %s", fileRecord.getName(), fileRecord.getMembershipNo(),
                        fileRecord.getProject(), fileRecord.getProjectDate(), fileRecord.getSubCommittee(), fileRecord.getProjectPeriod()));

                showNotificationAlert("Already there is a record with these information. Please check again", Alert.AlertType.ERROR);
                return; // Skip DB insert if duplicate is found
            }
            dbHandler.deleteRecord(fileRecord);
            dbHandler.insertData(newRecord); // You must have this method in DatabaseHandler

            String recordInfoTxt = newRecord.getName() + " - " +
                    newRecord.getMembershipNo() + " - " +
                    newRecord.getProject() + " - " +
                    newRecord.getProjectDate() + " - " +
                    newRecord.getSubCommittee() + " - " +
                    newRecord.getProjectPeriod();
            dbHandler.logAuditTrail("EDIT", recordInfoTxt,commentArea.getText().trim(), initiatedByField.getText().trim(), approverField.getText().trim());
            refreshTableData();
            showNotificationAlert("Record updated successfully.", Alert.AlertType.INFORMATION);
        }
    }

    private void deleteRecord(FileRecord record) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Delete confirmation");
        dialog.setHeaderText("Confirm deletion of the record below  and provide approval details");

        String recordInfoTxt = record.getName() + " - " +
                record.getMembershipNo() + " - " +
                record.getProject() + " - " +
                record.getProjectDate() + " - " +
                record.getSubCommittee() + " - " +
                record.getProjectPeriod();
        // Summary label just after header
        Label recordLabel = new Label(recordInfoTxt);
        recordLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 10 0;");

        VBox container = new VBox(10);
        container.setPadding(new Insets(20, 150, 10, 10));

        // Approval fields
        GridPane approvalGrid = new GridPane();
        approvalGrid.setHgap(10);
        approvalGrid.setVgap(10);

        TextField initiatedByField = new TextField();
        TextField approverField = new TextField();
        TextArea commentArea = new TextArea();
        commentArea.setPrefRowCount(3);

        approvalGrid.add(new Label("Initiated By:"), 0, 0); approvalGrid.add(initiatedByField, 1, 0);
        approvalGrid.add(new Label("Approved By:"), 0, 1); approvalGrid.add(approverField, 1, 1);
        approvalGrid.add(new Label("Comment:"), 0, 2); approvalGrid.add(commentArea, 1, 2);

        // Combine record label + approval fields
        container.getChildren().addAll(recordLabel, new Separator(), approvalGrid);

        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (initiatedByField.getText().isBlank() || approverField.getText().isBlank() || commentArea.getText().isBlank()) {
                showNotificationAlert("All approval fields must be filled.", Alert.AlertType.WARNING);
                return;
            }

            dbHandler.deleteRecord(record);
            dbHandler.logAuditTrail("DELETE", recordInfoTxt,commentArea.getText().trim(), initiatedByField.getText().trim(), approverField.getText().trim());
            refreshTableData();
            showNotificationAlert("Record deleted successfully.", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleAuditTrailView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AuditTrailView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Audit Trail Logs");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to open audit trail view", e);
        }
    }

    private void handleUploadToDrive() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MS Word Files", "*.docx"));
        fileChooser.setTitle("Select generated Service Letter file");
        File file = fileChooser.showOpenDialog(new Stage());

        if (file == null) {
            logger.info("File selection cancelled.");
            return; // or return false, or exit the method early
        }

        uploadToGoogleDrive(file);
    }

    private void uploadToGoogleDrive(File fileToUpload) {
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Uploading to Google Drive...");
                GoogleDriveUploader.uploadFile(fileToUpload, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                updateMessage("Upload successful!");
                return null;
            }
        };

        progressIndicator.setVisible(true);
        statusLabel.setVisible(true);
        statusLabel.textProperty().bind(uploadTask.messageProperty());

        uploadTask.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            statusLabel.textProperty().unbind();
            statusLabel.setText("Upload complete.");
        });

        uploadTask.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            statusLabel.textProperty().unbind();
            statusLabel.setText("Upload failed: " + uploadTask.getException().getMessage());
        });

        new Thread(uploadTask).start();
    }


}

