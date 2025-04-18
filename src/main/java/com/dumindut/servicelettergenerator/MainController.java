package com.dumindut.servicelettergenerator;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.geometry.Insets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
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
    private Button btnClean;
    @FXML
    private Button btnFilter;
    @FXML
    private Button btnClearFilter;
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
    private final DatabaseHandler dbHandler = new DatabaseHandler();
    private final Validator validator = new Validator();
    private final ExcelProcessor excelProcessor = new ExcelProcessor();
    private DocumentGenerator wordToPDFGenerator = new DocumentGenerator();
    private List<String> documentGenerationErrors = new ArrayList<>();

    @FXML
    public void initialize() {
        progressIndicator.setVisible(false);
        statusLabel.setVisible(false);
        btnUpload.setOnAction(e -> handleUpload());
        btnGenerate.setOnAction(e -> handleGenerate());
        btnGenerateAll.setOnAction(e -> handleGenerateAll());
        btnClean.setOnAction(e -> handleClean());
        btnFilter.setOnAction(e -> applyFilter());
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
        alert.setContentText("Steps to troubleshoot issues go here...");
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
        alert.setHeaderText(" Service Letter Generator");
        alert.setContentText("This application was developed to ease the Service Letter issuing process. Earlier, " +
                "a separate Service Letters were issued for each contribution. This application will save the " +
                "labour cost, stationary cost, printing cost, etc. ");
        alert.showAndWait();
    }

    private void handleUpload() {
        logger.debug("Upload button clicked");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xls", "*.xlsx"));
        fileChooser.setTitle("Select Excel File");

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            processExcelFile(file);
        }
    }

    private void processExcelFile(File file) {
        validator.resetErrors();
        if (validator.validateFile(file)) {
            excelProcessor.processExcel(file, dbHandler);

            // Refresh filteredData to reflect new records
            filteredData.setPredicate(null); // Reset predicate to allow new data
            filteredData = new FilteredList<>(dbHandler.getAllRecords(), p -> true);
            tableView.setItems(filteredData);
        } else {
            String title = "Excel File Validation Errors";
            String errorLabelTxt = "The following errors were found in the uploaded Excel file:";
            ErrorView.showErrors(validator.getErrors(), title, errorLabelTxt); // Show errors in a pop-up
        }
    }

    private void handleGenerate() {
        logger.debug("Generate PDFs clicked");

        ObservableList<FileRecord> dataSource = tableView.getItems();

        //generateSingleDocumentForAllItemsForMember(dataSource);
        File directoryToSave = getSaveLocation();
        boolean success = generateDocumentsForMember(dataSource, directoryToSave);

        if(success) {
            showNotificationAlert(INFO_DOC_GEN_SUCCESS, Alert.AlertType.INFORMATION);
        } else {
            showNotificationAlert("Error occurred when generating the documents", Alert.AlertType.ERROR);
        }
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

    private boolean generateDocumentsForMember(ObservableList<FileRecord> dataSource, File directoryToSave) {

        if(dataSource.isEmpty()){
            logger.error("No data in the table view");
            showNotificationAlert("No data in the table view.", Alert.AlertType.ERROR);
            return false;
        }

        documentGenerationErrors.clear();
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
                System.out.println("First IF - i= " + i + " | TotalCount= " + totalCount);
                tableData.add(chunkDbKeys);
                tableDataChunks.add(tableData);
                i=0;
                chunkDbKeys = new ArrayList<>();
                tableData = new ArrayList<>();
                tableData.add(headerData);
            } else {
                System.out.println("Second IF - i= " + i + " | TotalCount= " + totalCount);
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
            logger.error("filteredData is not for a single user");
            showNotificationAlert("Table contains information for multiple users. Please filter the data for a single user.", Alert.AlertType.ERROR);
            return false;
        }

        if(singularCheckForProjectPeriod.size() != 1) {
            logger.error("filteredData is not for a single project period");
            showNotificationAlert("Table contains information for multiple project periods. Please filter the data for a single project period.", Alert.AlertType.ERROR);
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
                    logger.error("Document Id DB update failed for User["+ firstRecord.getMembershipNo() + "] - DB key - " + dbKey + "  | Document Id - " + docId);
                    return fileGenerationSuccess;
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
        return membershipNo + "-" + System.currentTimeMillis();
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

    private void handleGenerateAll() {
        //Get all unique membership nos from the DB.
        List<String> membershipIds = dbHandler.getUniqueMembershipNos();
        File directoryToSave = getSaveLocation();

        //For each membership no, create a datasource and generate documents
        boolean success = true;
        for (String membershipNo : membershipIds) {
            ObservableList<FileRecord> datasource = dbHandler.getRecordsByMembershipNo(membershipNo);
            success = success && generateDocumentsForMember(datasource, directoryToSave);
        }

        if(success) {
            showNotificationAlert(INFO_DOC_GEN_SUCCESS, Alert.AlertType.INFORMATION);
        } else {
            ErrorView.showErrors(documentGenerationErrors, "Document Generation Errors", "Following issues encountered when generating the documents");
        }
    }

    private void handleClean() {
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

        nameCol.setPrefWidth(200);
        membershipNoCol.setPrefWidth(150);
        projectCol.setPrefWidth(200);
        projectCodeCol.setPrefWidth(150);
        projectDateCol.setPrefWidth(100);
        subCommitteeCol.setPrefWidth(150);
        projectPeriodCol.setPrefWidth(150);
        documentIdCol.setPrefWidth(180);
        documentDateCol.setPrefWidth(100);
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
            MenuItem copyItem = new MenuItem("Copy");

            copyItem.setOnAction(e -> {
                String cellValue = cell.getText();
                if (cellValue != null) {
                    copyToClipboard(cellValue);
                }
            });

            contextMenu.getItems().add(copyItem);

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
}

