package com.dumindut.servicelettergenerator;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FileRecord {

    private StringProperty name;
    private StringProperty membershipNo;
    private StringProperty project;
    private StringProperty projectCode;
    private StringProperty projectDate;
    private StringProperty subCommittee;
    private StringProperty documentId;
    private StringProperty documentDate;
    private StringProperty projectPeriod;
    private StringProperty dbPrimaryKeyId;
    private StringProperty lastUpdatedTime;
    private StringProperty approvedBy;


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

    public void setName(String name) {
        this.name = new SimpleStringProperty(name);
    }

    public void setMembershipNo(String membershipNo) {
        this.membershipNo = new SimpleStringProperty(membershipNo);
    }

    public void setProject(String project) {
        this.project = new SimpleStringProperty(project);
    }

    public void setProjectDate(String projectDate) {
        this.projectDate = new SimpleStringProperty(projectDate);
    }

    public void setSubCommittee(String subCommittee) {
        this.subCommittee = new SimpleStringProperty(subCommittee);
    }

    public void setProjectPeriod(String projectPeriod) {
        this.projectPeriod = new SimpleStringProperty(projectPeriod);
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = new SimpleStringProperty(approvedBy);
    }

    public void setLastUpdatedTime(String lastUpdatedTime) {
        this.lastUpdatedTime = new SimpleStringProperty(lastUpdatedTime);
    }
}
