package com.dumindut.servicelettergenerator;

import javafx.collections.ObservableList;

import java.util.List;

public interface DatabaseHandler {
    void createTables();

    void insertData(FileRecord fileRecord);

    ObservableList<FileRecord> getAllRecords();

    boolean isDuplicate(FileRecord fileRecord);

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

    void logAuditTrail(String action, String description, String comment, String initiatedBy, String approvedBy);

    ObservableList<AuditTrailRecord> getAllAuditLogs();

}
