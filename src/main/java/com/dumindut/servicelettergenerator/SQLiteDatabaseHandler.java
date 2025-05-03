package com.dumindut.servicelettergenerator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SQLiteDatabaseHandler implements DatabaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteDatabaseHandler.class);
    private static final String DB_URL = "jdbc:sqlite:slgdb.db";

    public SQLiteDatabaseHandler() {
        createTables();
    }

    public void createTables() {
        createFileRecordTable();
        createAuditTable();
    }

    public void createFileRecordTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS filerecords (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                membership_no TEXT NOT NULL,
                project TEXT,
                project_code TEXT,
                project_date TEXT,
                sub_committee TEXT,
                document_id TEXT,
                document_date TEXT,
                project_period TEXT,
                last_updated_time TEXT,
                approved_by TEXT,
                UNIQUE(name, membership_no, project, project_date, sub_committee,project_period)  -- Prevents duplicate
            )""";
        try (var conn = DriverManager.getConnection(SQLiteDatabaseHandler.DB_URL);
             var stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            SQLiteDatabaseHandler.logger.error(e.getMessage(), e);
        }
    }

    public void createAuditTable() {
        String auditTrailSQL = """
        CREATE TABLE IF NOT EXISTS audit_trail (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            actioned_time TEXT,
            action TEXT,
            description TEXT,
            user_comment TEXT,
            initiated_by TEXT,
            approved_by TEXT
        )""";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(auditTrailSQL);
        } catch (SQLException e) {
            logger.error("Failed to create audit_trail table", e);
        }
    }

    @Override
    public void insertData(FileRecord fileRecord) {

        String sql = """
                INSERT INTO filerecords (name, membership_no, project, project_date, sub_committee, project_period,last_updated_time,approved_by) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";
        try (var conn = DriverManager.getConnection(DB_URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileRecord.getName());
            pstmt.setString(2, fileRecord.getMembershipNo());
            pstmt.setString(3, fileRecord.getProject());
            pstmt.setString(4, fileRecord.getProjectDate()); // Storing as TEXT (ISO format)
            pstmt.setString(5, fileRecord.getSubCommittee());
            pstmt.setString(6, fileRecord.getProjectPeriod());
            pstmt.setString(7,fileRecord.getLastUpdatedTime());
            pstmt.setString(8,fileRecord.getApprovedBy());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public boolean isDuplicate(FileRecord fileRecord) {
        String sql = "SELECT COUNT(*) FROM filerecords WHERE name = ? AND membership_no = ? AND project = ? AND project_date = ? AND sub_committee = ? AND project_period = ?";
        try (var conn = DriverManager.getConnection(SQLiteDatabaseHandler.DB_URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileRecord.getName());
            pstmt.setString(2, fileRecord.getMembershipNo());
            pstmt.setString(3, fileRecord.getProject());
            pstmt.setString(4, fileRecord.getProjectDate());
            pstmt.setString(5, fileRecord.getSubCommittee());
            pstmt.setString(6, fileRecord.getProjectPeriod());
            var rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;  // If count > 0, record exists
        } catch (SQLException e) {
            SQLiteDatabaseHandler.logger.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public ObservableList<FileRecord> getAllRecords() {
        ObservableList<FileRecord> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM filerecords";
        try (var conn = DriverManager.getConnection(DB_URL);
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new FileRecord(
                        rs.getString("name"),
                        rs.getString("membership_no"),
                        rs.getString("project"),
                        rs.getString("project_code"),
                        rs.getString("project_date"),
                        rs.getString("sub_committee"),
                        rs.getString("document_id"),
                        rs.getString("document_date"),
                        rs.getString("project_period"),
                        rs.getString("id"),
                        rs.getString("last_updated_time"),
                        rs.getString("approved_by")));
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return list;
    }

    @Override
    public String getExistingDocumentId(String membershipNo) {
        String sql = "SELECT document_id FROM filerecords WHERE membership_no = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, membershipNo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("document_id");
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean updateNewDocumentId(String membershipNo, String documentId) {
        String sql = "UPDATE filerecords SET document_id = ? WHERE membership_no = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, documentId);
            stmt.setString(2, membershipNo);
            int updatedRows = stmt.executeUpdate();
            if (updatedRows > 0) {
                return true;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public String getExistingDocumentDate(String membershipNo) {
        String sql = "SELECT document_date FROM filerecords WHERE membership_no = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, membershipNo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("document_date");
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean updateNewDocumentDate(String membershipNo, String documentDate) {
        String sql = "UPDATE filerecords SET document_date = ? WHERE membership_no = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, documentDate);
            stmt.setString(2, membershipNo);
            int updatedRows = stmt.executeUpdate();
            if (updatedRows > 0) {
                return true;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean cleanDatabase() {
        boolean isSuccess = false;
        String sql = "DELETE FROM filerecords";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            isSuccess = true;
            logger.info("All records deleted from filerecords table.");
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return isSuccess;
    }

    @Override
    public boolean updateDBDocId(String dbKey, String docId) {
        String sql = "UPDATE filerecords SET document_id = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, docId);
            stmt.setString(2, dbKey);
            int updatedRows = stmt.executeUpdate();
            if (updatedRows > 0) {
                return true;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<String> getUniqueMembershipNos() {
        List<String> membershipIds = new ArrayList<>();
        String sql = "SELECT DISTINCT membership_no FROM filerecords WHERE membership_no IS NOT NULL AND membership_no != ''";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                membershipIds.add(rs.getString("membership_no"));
            }
        } catch (SQLException e) {
            logger.error("Error fetching unique membership numbers", e);
        }

        return membershipIds;
    }


    @Override
    public ObservableList<FileRecord> getRecordsByMembershipNo(String membershipNo) {
        ObservableList<FileRecord> records = FXCollections.observableArrayList();
        String sql = "SELECT * FROM filerecords WHERE membership_no = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, membershipNo);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                FileRecord record = new FileRecord(
                        rs.getString("name"),
                        rs.getString("membership_no"),
                        rs.getString("project"),
                        rs.getString("project_code"),
                        rs.getString("project_date"),
                        rs.getString("sub_committee"),
                        rs.getString("document_id"),
                        rs.getString("document_date"),
                        rs.getString("project_period"),
                        rs.getString("id"),
                        rs.getString("last_updated_time"),
                        rs.getString("approved_by")
                );
                records.add(record);
            }

        } catch (SQLException e) {
            logger.error("Error fetching records for membership number: " + membershipNo, e);
        }

        return records;
    }

    @Override
    public void deleteRecord(FileRecord record) {
        String sql = "DELETE FROM filerecords WHERE name = ? AND membership_no = ? AND project = ? AND project_date = ? AND sub_committee = ? AND project_period = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, record.getName());
            pstmt.setString(2, record.getMembershipNo());
            pstmt.setString(3, record.getProject());
            pstmt.setString(4, record.getProjectDate());
            pstmt.setString(5, record.getSubCommittee());
            pstmt.setString(6, record.getProjectPeriod());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting record", e);
        }
    }

    @Override
    public void updateRecord(FileRecord record) {
    }


    @Override
    public void logAuditTrail(String action, String description, String comment, String initiatedBy, String approvedBy) {
        String sql = "INSERT INTO audit_trail (actioned_time, action, description, user_comment, initiated_by, approved_by) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.setString(2, action);
            pstmt.setString(3, description);
            pstmt.setString(4, comment);
            pstmt.setString(5, initiatedBy);
            pstmt.setString(6, approvedBy);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error logging audit trail", e);
        }
    }

    @Override
    public ObservableList<AuditTrailRecord> getAllAuditLogs() {
        ObservableList<AuditTrailRecord> logs = FXCollections.observableArrayList();
        String sql = "SELECT * FROM audit_trail ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new AuditTrailRecord(
                        rs.getString("actioned_time"),
                        rs.getString("action"),
                        rs.getString("description"),
                        rs.getString("user_comment"),
                        rs.getString("initiated_by"),
                        rs.getString("approved_by")
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to load audit trail logs", e);
        }
        return logs;
    }

}
