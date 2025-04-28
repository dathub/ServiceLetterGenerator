package com.dumindut.servicelettergenerator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.dumindut.servicelettergenerator.Defs.DATE_PATTERN;

public class ExcelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ExcelProcessor.class);
    public void processExcel(File file, DatabaseHandler dbHandler, String approvalComment) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> iterator = sheet.iterator();
            iterator.next(); // Skip header

            while (iterator.hasNext()) {
                Row row = iterator.next();
                if (row.getPhysicalNumberOfCells() < Defs.COL_COUNT) continue;

                String name = getCellValue(row.getCell(0));
                String membershipNo = getCellValue(row.getCell(1));
                String project = getCellValue(row.getCell(2));
                String projectCode = getCellValue(row.getCell(3));
                String projectDate = getCellValue(row.getCell(4));
                String subCommittee = getCellValue(row.getCell(5));
                String projectPeriod = getCellValue(row.getCell(6));

                String lastUpdatedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                FileRecord fileRecord = new FileRecord(name, membershipNo, project,projectCode , projectDate,
                        subCommittee, "", "", projectPeriod, "",lastUpdatedTime,approvalComment);

                logger.debug(fileRecord.toString());
                dbHandler.insertData(fileRecord);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static String getCellValue(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield new SimpleDateFormat(DATE_PATTERN).format(cell.getDateCellValue());
                } else {
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }
}
