package com.dumindut.servicelettergenerator;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FileRecord {

    private final StringProperty name;
    private final StringProperty membershipNo;
    private final StringProperty project;
    private final StringProperty projectCode;
    private final StringProperty projectDate;
    private final StringProperty subCommittee;
    private final StringProperty documentId;
    private final StringProperty documentDate;
    private final StringProperty projectPeriod;
    private final StringProperty dbPrimaryKeyId;
    private final StringProperty lastUpdatedTime;
    private final StringProperty approvedBy;


    public FileRecord(String name, String membershipNo, String project, String projectCode, String projectDate,
                      String subCommittee, String documentId, String documentDate, String projectPeriod, String dbKey,
                      String lastUpdatedTime, String approvedBy) {
        this.name = new SimpleStringProperty(name);
        this.membershipNo = new SimpleStringProperty(membershipNo);
        this.project = new SimpleStringProperty(project);
        this.projectCode = new SimpleStringProperty(projectCode);
        this.projectDate = new SimpleStringProperty(projectDate);
        this.subCommittee = new SimpleStringProperty(subCommittee);
        this.documentId = new SimpleStringProperty(documentId);
        this.documentDate = new SimpleStringProperty(documentDate);
        this.projectPeriod = new SimpleStringProperty(projectPeriod);
        this.dbPrimaryKeyId = new SimpleStringProperty(dbKey);
        this.lastUpdatedTime = new SimpleStringProperty(lastUpdatedTime);
        this.approvedBy = new SimpleStringProperty(approvedBy);
    }

    // JavaFX Properties (for TableView binding)
    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty membershipNoProperty() {
        return membershipNo;
    }

    public StringProperty projectProperty() {
        return project;
    }

    public StringProperty projectCodeProperty() {
        return projectCode;
    }

    public StringProperty projectDateProperty() {
        return projectDate;
    }

    public StringProperty subCommitteeProperty() {
        return subCommittee;
    }

    public StringProperty documentIdProperty() {
        return documentId;
    }

    public StringProperty documentDateProperty() {
        return documentDate;
    }

    public StringProperty projectPeriodProperty() {
        return projectPeriod;
    }

    public StringProperty dbPrimaryKeyIdProperty() {
        return dbPrimaryKeyId;
    }

    public StringProperty lastUpdatedTimeProperty() {
        return lastUpdatedTime;
    }

    public StringProperty approvedByProperty() {
        return approvedBy;
    }


    // Standard Getter Methods (for database storage)
    public String getName() {
        return name.get();
    }

    public String getMembershipNo() {
        return membershipNo.get();
    }

    public String getProject() {
        return project.get();
    }

    public String getProjectCode() {
        return projectCode.get();
    }

    public String getProjectDate() {
        return projectDate.get();
    }

    public String getSubCommittee() {
        return subCommittee.get();
    }

    public String getDocumentId() {
        return documentId.get();
    }

    public String getDocumentDate() {
        return documentDate.get();
    }

    public String getProjectPeriod() {
        return projectPeriod.get();
    }

    public String getDbPrimaryKeyId() {
        return dbPrimaryKeyId.get();
    }

    public String getLastUpdatedTime() {
        return lastUpdatedTime.get();
    }

    public String getApprovedBy() {
        return approvedBy.get();
    }

}
