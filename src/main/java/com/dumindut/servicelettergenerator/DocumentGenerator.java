package com.dumindut.servicelettergenerator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.poi.xwpf.usermodel.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import static com.dumindut.servicelettergenerator.Defs.*;

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

        // Replace text in paragraphs (top-level)
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceTextInParagraph(paragraph, replacements);
        }

        // Replace text in tables (cell paragraphs)
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceTextInParagraph(paragraph, replacements);
                    }
                }
            }
        }

        // Insert table at VAR_SERVICE_CONTRIBUTION_TABLE
        insertTableAtPlaceholder(document, VAR_CONTRIBUTION_TABLE, tableData);

        insertQRImage(document, replacements);

        // Write to output file
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            document.write(fos);
        }

        document.close();
        inputStream.close();
    }

    private void insertQRImage(XWPFDocument document, Map<String, String> replacements) throws Exception {
        String memName = replacements.get(VAR_MEMBER_NAME);
        String memId = replacements.get(VAR_MEMBERSHIP_NO);
        String letterNo = replacements.get(VAR_LETTER_NO);
        String letterDate = replacements.get(VAR_LETTER_DATE);
        String projectCount = replacements.get(VAR_ROW_COUNT);

        String qrData = "Member Name: " + memName + ",\n Member Id: " + memId + ",\n Letter No: " + letterNo + ",\n Letter Date: "
                + letterDate + ",\n Project Count: " + projectCount;

        ByteArrayOutputStream qrOutput = generateQRCode(qrData, 64, 64);
        byte[] qrBytes = qrOutput.toByteArray();

        // Call cleaner method
        insertQRImageParagraph(document, qrBytes, 64, 64);
    }

    private void insertQRImageParagraph(XWPFDocument document, byte[] imageBytes, int widthPx, int heightPx) throws Exception {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.RIGHT);

        XWPFRun run = paragraph.createRun();
        int emuWidth = widthPx * 9525;
        int emuHeight = heightPx * 9525;

        try (InputStream imgInput = new ByteArrayInputStream(imageBytes)) {
            run.addPicture(imgInput, Document.PICTURE_TYPE_PNG, "qr.png", emuWidth, emuHeight);
        }

        // Add caption below
        XWPFRun captionRun = paragraph.createRun();
        captionRun.addBreak();  // moves text to a new line
        captionRun.setText("Verify me");
        captionRun.setFontSize(8);  // optional: smaller font
        captionRun.setItalic(true); // optional: italic for styling
    }



    private void insertQRImageIntoCell(XWPFDocument document, int tableIndex, int rowIndex, int cellIndex, byte[] imageBytes, int widthPx, int heightPx) throws Exception {
        if (document.getTables().size() <= tableIndex) return;
        XWPFTable table = document.getTables().get(tableIndex);
        if (table.getNumberOfRows() <= rowIndex) return;
        XWPFTableRow row = table.getRow(rowIndex);
        if (row.getTableCells().size() <= cellIndex) return;
        XWPFTableCell cell = row.getCell(cellIndex);

        // ✅ Always add a new paragraph
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();

        int emuWidth = widthPx * 9525;
        int emuHeight = heightPx * 9525;

        try (InputStream imgInput = new ByteArrayInputStream(imageBytes)) {
            run.addPicture(imgInput, Document.PICTURE_TYPE_PNG, "qr.png", emuWidth, emuHeight);
        }

        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        paragraph.setAlignment(ParagraphAlignment.CENTER);
    }

    /**
     * Replaces text inside an XWPFParagraph by modifying its runs.
     */

    private void replaceTextInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        // Collect full paragraph text
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) {
                fullText.append(text);
            }
        }

        String paragraphText = fullText.toString();
        boolean found = false;

        // Perform replacements in the combined text
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = entry.getKey();
            String replacement = entry.getValue();

            if (paragraphText.contains(placeholder)) {
                paragraphText = paragraphText.replace(placeholder, replacement);
                found = true;
            }
        }

        if (found) {
            // Cache formatting from first run before clearing all runs
            XWPFRun templateRun = runs.get(0);
            int fontSize = templateRun.getFontSize();
            boolean isBold = templateRun.isBold();
            boolean isItalic = templateRun.isItalic();
            UnderlinePatterns ulPattern = templateRun.getUnderline();
            String fontFamily = templateRun.getFontFamily();
            String color = templateRun.getColor();

            // Remove all runs from paragraph
            for (int i = runs.size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }

            // Create a new run with replaced text and preserved formatting
            XWPFRun newRun = paragraph.createRun();
            newRun.setText(paragraphText);

            if (fontSize > 0) newRun.setFontSize(fontSize);
            if (isBold) newRun.setBold(true);
            if (isItalic) newRun.setItalic(true);
            if(ulPattern != null) newRun.setUnderline(ulPattern);
            if (fontFamily != null) newRun.setFontFamily(fontFamily);
            if (color != null) newRun.setColor(color);
        }
    }

    /**
     * Inserts a table at the location of a placeholder (e.g., V_8) in the Word document.
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
        List<XWPFRun> runs = targetParagraph.getRuns();
        StringBuilder combinedText = new StringBuilder();

        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) {
                combinedText.append(text);
            }
        }

        String newText = combinedText.toString().replace(placeholder, "");

        if (!newText.equals(combinedText.toString())) {
            // Copy formatting from first run
            XWPFRun templateRun = runs.get(0);
            int fontSize = templateRun.getFontSize();
            boolean isBold = templateRun.isBold();
            boolean isItalic = templateRun.isItalic();
            UnderlinePatterns ulPattern = templateRun.getUnderline();
            String fontFamily = templateRun.getFontFamily();
            String color = templateRun.getColor();

            // Remove all runs
            for (int i = runs.size() - 1; i >= 0; i--) {
                targetParagraph.removeRun(i);
            }

            // Add single run with placeholder removed and formatting preserved
            XWPFRun newRun = targetParagraph.createRun();
            newRun.setText(newText);

            if (fontSize > 0) newRun.setFontSize(fontSize);
            if (isBold) newRun.setBold(true);
            if (isItalic) newRun.setItalic(true);
            if(ulPattern != null) newRun.setUnderline(ulPattern);
            if (fontFamily != null) newRun.setFontFamily(fontFamily);
            if (color != null) newRun.setColor(color);
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

    public ByteArrayOutputStream generateQRCode(String data, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);

        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Fill the image manually: black (0x000000) for bits set, white (0xFFFFFF) otherwise
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF;
                qrImage.setRGB(x, y, color);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "png", baos);
        return baos;
    }


//    public ByteArrayOutputStream generateQRCode(String data, int width, int height) throws WriterException, IOException {
//        QRCodeWriter qrCodeWriter = new QRCodeWriter();
//        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
//
//        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF); // Black or white
//            }
//        }
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(qrImage, "png", baos);
//        return baos;
//    }


//    public ByteArrayOutputStream generateQRCode(String data, int width, int height) throws WriterException, IOException {
//        QRCodeWriter qrCodeWriter = new QRCodeWriter();
//        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
//
//        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF); // black or white
//            }
//        }
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(qrImage, "png", baos);
//        return baos;
//    }

    private void replacePlaceholderWithImage(XWPFParagraph paragraph, String placeholder, byte[] imageBytes, int widthPx, int heightPx) throws Exception {
        if (paragraph == null || !paragraph.getText().contains(placeholder)) return;

        // Step 1: Rebuild full text across runs
        List<XWPFRun> runs = paragraph.getRuns();
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) fullText.append(text);
        }

        String[] parts = fullText.toString().split(placeholder, 2);

        // Step 2: Remove all runs
        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // Step 3: Add text before placeholder
        if (parts.length > 0 && !parts[0].isEmpty()) {
            XWPFRun before = paragraph.createRun();
            before.setText(parts[0]);
        }

        // Step 4: Register image with document
        int emuWidth = widthPx * 9525;
        int emuHeight = heightPx * 9525;
        int pictureType = Document.PICTURE_TYPE_PNG;

        XWPFDocument doc = paragraph.getDocument();
        String picId;
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            picId = doc.addPictureData(is, pictureType);
        }

        // Step 5: Add image with correct ID and name
        XWPFRun imageRun = paragraph.createRun();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            imageRun.addPicture(
                    is,
                    pictureType,
                    "qr.png",
                    emuWidth,
                    emuHeight
            );
        }

        // Step 6: Add text after placeholder
        if (parts.length > 1 && !parts[1].isEmpty()) {
            XWPFRun after = paragraph.createRun();
            after.setText(parts[1]);
        }
    }



}