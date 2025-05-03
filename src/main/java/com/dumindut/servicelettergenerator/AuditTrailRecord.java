package com.dumindut.servicelettergenerator;

import javafx.beans.property.SimpleStringProperty;

public class AuditTrailRecord {
    private final SimpleStringProperty actionedTime;
    private final SimpleStringProperty action;
    private final SimpleStringProperty description;
    private final SimpleStringProperty userComment;
    private final SimpleStringProperty initiatedBy;
    private final SimpleStringProperty approvedBy;

    public AuditTrailRecord(String time, String action, String description, String comment, String initiatedBy, String approvedBy) {
        this.actionedTime = new SimpleStringProperty(time);
        this.action = new SimpleStringProperty(action);
        this.description = new SimpleStringProperty(description);
        this.userComment = new SimpleStringProperty(comment);
        this.initiatedBy = new SimpleStringProperty(initiatedBy);
        this.approvedBy = new SimpleStringProperty(approvedBy);
    }

    public String getActionedTime() { return actionedTime.get(); }
    public String getAction() { return action.get(); }
    public String getDescription() { return description.get(); }
    public String getUserComment() { return userComment.get(); }
    public String getInitiatedBy() { return initiatedBy.get(); }
    public String getApprovedBy() { return approvedBy.get(); }

    public SimpleStringProperty actionedTimeProperty() {
        return actionedTime;
    }

    public SimpleStringProperty actionProperty() {
        return action;
    }

    public SimpleStringProperty descriptionProperty() {
        return description;
    }

    public SimpleStringProperty userCommentProperty() {
        return userComment;
    }

    public SimpleStringProperty initiatedByProperty() {
        return initiatedBy;
    }

    public SimpleStringProperty approvedByProperty() {
        return approvedBy;
    }
}

