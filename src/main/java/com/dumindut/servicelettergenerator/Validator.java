package com.dumindut.servicelettergenerator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.dumindut.servicelettergenerator.Defs.*;
import static com.dumindut.servicelettergenerator.ExcelProcessor.getCellValue;

public class Validator {

    private static final Logger logger = LoggerFactory.getLogger(Validator.class);
    private List<String> errors = new ArrayList<>();


    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);

    static {
        DATE_FORMAT.setLenient(false); // Strict date format validation
    }

    public boolean validateFile(File file) {
        if(!validateFileSize(file)) {
            addError("Invalid file! Ensure it is less than 1MB");
            return false;
        }

        return validateFileContent(file);
    }

    public boolean validateFileContent(File file) {
        boolean isSuccess = false;
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                logger.error("Error: Missing header row in Excel file");
                addError("Missing header row in Excel file");
                return false;
            }

            // Check if the columns exist and are in the correct order
            for (int i = 0; i < REQUIRED_COLUMNS.size(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null || !cell.getStringCellValue().trim().equalsIgnoreCase(REQUIRED_COLUMNS.get(i))) {
                    logger.error("Column '" + REQUIRED_COLUMNS.get(i) + "' is missing or out of order at position " + (i + 1));
                    addError("Column '" + REQUIRED_COLUMNS.get(i) + "' is missing or out of order at position " + (i + 1));
                    return false;
                }
            }

            // Validate max number of rows
            if(sheet.getLastRowNum() > MAX_FILE_ROW_COUNT) {
                logger.error("Error: File row count breaches the max row count limit");
                addError("File row count breaches the max row count limit");
                return false;
            }
            // Validate data types for each row
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                for (int i = 0; i < REQUIRED_COLUMNS.size(); i++) {

                    String columnName = REQUIRED_COLUMNS.get(i);
                    Cell cell = row.getCell(i);

                    if (cell == null) {
                        logger.error("Error: Missing value in column '" + columnName + "' at row " + (row.getRowNum() + 1));
                        addError("Missing value in column '" + columnName + "' at row " + (row.getRowNum() + 1));
                        return false;
                    }

                    if (columnName.equals(COL_PROJECT)) {
                        String projectValue = getCellValue(cell);
                        if (projectValue.length() > 50) {
                            addError("Project value exceeds 50 characters at row " + (row.getRowNum() + 1));
                        }
                    }

                    // Validate PROJECT DATE column as DATE type
//                    if (columnName.equals(COL_PROJECT_DATE)) {
//                        if (cell.getCellType() != CellType.NUMERIC || !DateUtil.isCellDateFormatted(cell)) {
//                            logger.error("Error: Column '" + COL_PROJECT_DATE + "' must be a valid DATE at row " + (row.getRowNum() + 1));
//                            addError("Column '" + COL_PROJECT_DATE + "' must be a valid DATE at row " + (row.getRowNum() + 1));
//                            return false;
//                        }
//                    } else {
//                        // Other columns should be STRING
//                        if (cell.getCellType() != CellType.STRING && cell.getCellType() != CellType.NUMERIC) {
//                            logger.error("Error: Column '" + REQUIRED_COLUMNS.get(i) + "' must be TEXT at row " + (row.getRowNum() + 1));
//                            addError("Error: Column '" + REQUIRED_COLUMNS.get(i) + "' must be TEXT at row " + (row.getRowNum() + 1));
//                            return false;
//                        }
//                    }

                    // Validate PROJECT DATE format
                    if (columnName.equals(COL_PROJECT_DATE)) {
                        String dateString = getCellValue(cell);
                        if (!isValidDateFormat(dateString)) {
                            addError("Column '" + COL_PROJECT_DATE + "' must be in " + DATE_PATTERN + " format at row " + (row.getRowNum() + 1) + " (Found: " + dateString + ")");
                        }
                    } else {
                        // Other columns should be STRING
                        if (cell.getCellType() != CellType.STRING && cell.getCellType() != CellType.NUMERIC) {
                            addError("Column '" + columnName + "' must be TEXT at row " + (row.getRowNum() + 1));
                        }
                    }
                }
            }

            if(errors.isEmpty()) {
                isSuccess = true; // File is valid
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return isSuccess;
    }

    private boolean isValidDateFormat(String date) {
        boolean isValid = false;
        try {
            DATE_FORMAT.parse(date);
            isValid = true;
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        return isValid;
    }

    private boolean validateFileSize(File file) {
        return file.length() <= 1_000_000; // 1MB limit
    }


    public void addError(String err) {
        errors.add(err);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void resetErrors() {
        errors.clear();
    }
}

