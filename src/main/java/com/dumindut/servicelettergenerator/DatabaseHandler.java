package com.dumindut.servicelettergenerator;

import javafx.collections.ObservableList;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public interface DatabaseHandler {
    void createTable();

    void insertData(FileRecord fileRecord);

    ObservableList<FileRecord> getAllRecords();

    boolean isDuplicate(String name, String membershipNo, String project, String projectDate, String subCommittee, String projectPeriod);

    String getExistingDocumentId(String membershipNo);

    boolean updateNewDocumentId(String membershipNo, String documentId);

    String getExistingDocumentDate(String membershipNo);

    boolean updateNewDocumentDate(String membershipNo, String documentDate);

    boolean cleanDatabase();

    boolean updateDBDocId(String dbKey, String docId);

    List<String> getUniqueMembershipNos();

    ObservableList<FileRecord> getRecordsByMembershipNo(String membershipNo);

    void deleteRecord(FileRecord record);

    void updateRecord(FileRecord record);
}
