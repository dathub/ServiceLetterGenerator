package com.dumindut.servicelettergenerator;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DocumentGenerator.class);

    public boolean generatePdf(Map<String, String> replacements, List<List<String>> tableData, File saveDirectory, String generatedFileName) {

        if(tableData == null || tableData.isEmpty()){
            logger.error("Table Data is empty");
            return false;
        }

        if(generatedFileName != null && !generatedFileName.trim().isEmpty() && !generatedFileName.endsWith(".docx")) {
            generatedFileName += ".docx";
        }


        if (saveDirectory == null) {
            logger.error("Save directory is null");
            return false;
        }

        File fileToSave = new File(saveDirectory, generatedFileName);

        boolean isSuccess = false;

        String inputFilePath = "/templates/LetterTemplate.docx";  // Template file in resources

        String outputWordFile = fileToSave.getAbsoluteFile().getAbsolutePath();

        logger.debug("GeneratedFilePath: " + outputWordFile);

        try {
            replaceTextInWord(inputFilePath, outputWordFile, replacements, tableData);
            logger.debug("Word document updated successfully with a table.");
            isSuccess = true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return isSuccess;
    }

    public void replaceTextInWord(String resourcePath, String outputFile, Map<String, String> replacements, List<List<String>> tableData) throws Exception {
        // Load template file from resources
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Template file not found in resources: " + resourcePath);
        }

        XWPFDocument document = new XWPFDocument(inputStream);

        // Replace text in paragraphs
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceTextInParagraph(paragraph, replacements);
        }

        // Replace text in tables
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceTextInParagraph(paragraph, replacements);
                    }
                }
            }
        }

        // Insert table at VAR_8
        insertTableAtPlaceholder(document, "VAR_8", tableData);

        // Write to output file
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            document.write(fos);
        }

        document.close();
        inputStream.close();
    }

    /**
     * Replaces text inside an XWPFParagraph by modifying its runs.
     */
    private void replaceTextInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text != null) {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    if (text.contains(entry.getKey())) {
                        text = text.replace(entry.getKey(), entry.getValue());
                        run.setText(text, 0); // Update the text in the run
                    }
                }
            }
        }
    }

    /**
     * Inserts a table at the location of a placeholder (e.g., VAR_8) in the Word document.
     */
    private void insertTableAtPlaceholder(XWPFDocument document, String placeholder, List<List<String>> tableData) {
        XWPFParagraph targetParagraph = null;

        // Find the paragraph containing the placeholder
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (paragraph.getText().contains(placeholder)) {
                targetParagraph = paragraph;
                break;
            }
        }

        if (targetParagraph == null) {
            logger.debug("Placeholder '" + placeholder + "' not found. Skipping table insertion.");
            return;
        }

        // Remove the placeholder text safely
        for (XWPFRun run : targetParagraph.getRuns()) {
            String text = run.getText(0);
            if (text != null && text.contains(placeholder)) {
                run.setText(text.replace(placeholder, ""), 0); // Clear placeholder
            }
        }

        // Use XML Cursor to insert table at the correct position
        XmlCursor cursor = targetParagraph.getCTP().newCursor();
        XWPFTable table = document.insertNewTbl(cursor);

        // 💡 Center the table
        table.setTableAlignment(TableRowAlign.CENTER); // High-level POI method

        // Also explicitly set alignment at XML level
        table.getCTTbl().getTblPr().addNewJc().setVal(STJcTable.CENTER);


        // Apply table styling (Borders)
        setTableBorders(table);

        for(int i=0; i < table.getNumberOfRows(); i++){
            table.removeRow(i);
        }

        // Create header row
        XWPFTableRow headerRow = table.createRow();
        for (String header : tableData.get(0)) {
            XWPFTableCell cell = headerRow.addNewTableCell();
            XWPFParagraph para = cell.getParagraphs().get(0);
            XWPFRun run = para.createRun();
            run.setBold(true);
            run.setText(header);
        }

        // Populate table with data (excluding headers)
        for (int i = 1; i < tableData.size(); i++) {
            XWPFTableRow tableRow = table.createRow();
            int celPos = 0;
            for (String cellData : tableData.get(i)) {
                XWPFTableCell cell = tableRow.getCell(celPos);
                cell.setText(cellData);
                celPos++;
            }
        }
    }

    /**
     * Applies borders to a table.
     */
    private void setTableBorders(XWPFTable table) {
        CTTblBorders borders = table.getCTTbl().getTblPr().addNewTblBorders();

        borders.addNewTop().setVal(STBorder.SINGLE);
        borders.addNewBottom().setVal(STBorder.SINGLE);
        borders.addNewLeft().setVal(STBorder.SINGLE);
        borders.addNewRight().setVal(STBorder.SINGLE);
        borders.addNewInsideH().setVal(STBorder.SINGLE);
        borders.addNewInsideV().setVal(STBorder.SINGLE);
    }
}