package com.dumindut.servicelettergenerator;

import java.util.List;

public class Defs {

    public static final int COL_COUNT = 5;

    public static final int MAX_FILE_ROW_COUNT = 20000;
    public static final int MAX_PAGE_ENTRY_COUNT = 10;
    public static final String COL_NAME = "NAME";
    public static final String COL_MEMBERSHIP_NO = "MEMBERSHIP NO";
    public static final String COL_PROJECT = "PROJECT";
    public static final String COL_PROJECT_CODE = "PROJECT CODE";
    public static final String COL_PROJECT_DATE = "PROJECT DATE";
    public static final String COL_SUB_COMMITTEE = "SUB COMMITTEE";
    public static final String COL_DOCUMENT_ID = "DOCUMENT ID";
    public static final String COL_PROJECT_PERIOD = "PROJECT PERIOD";

    public static final String ERROR_SYSTEM = "Internal error occurred. Take a screenshot/photo of the error and contact system admin.";
    public static final String INFO_DOC_GEN_SUCCESS = "Documents generated successfully";

    public static final List<String> REQUIRED_COLUMNS = List.of(
            COL_NAME,COL_MEMBERSHIP_NO,COL_PROJECT,COL_PROJECT_CODE,COL_PROJECT_DATE,COL_SUB_COMMITTEE,COL_PROJECT_PERIOD
    );

    public static final String DATE_PATTERN = "dd/MM/yyyy";

    public static final String VAR_MEMBER_NAME = "VAR_1";
    public static final String VAR_MEMBERSHIP_NO = "VAR_2";
    public static final String VAR_NIC = "VAR_3";
    public static final String VAR_LETTER_NO = "VAR_4";
    public static final String VAR_LETTER_DATE = "VAR_5";
    public static final String VAR_ROW_COUNT = "VAR_6";
    public static final String VAR_SERVICE_PERIOD = "VAR_7";

    public static final String DB_COL_ID = "id";
    public static final String DB_COL_NAME = "name";
    public static final String DB_COL_MEMBERSHIP_NO = "membership_no";
    public static final String DB_COL_PROJECT = "project";
    public static final String DB_COL_PROJECT_CODE = "project_code";
    public static final String DB_COL_PROJECT_DATE = "project_date";
    public static final String DB_COL_SUB_COMMITTEE = "sub_committee";
    public static final String DB_COL_DOCUMENT_ID = "document_id";
    public static final String DB_COL_DOCUMENT_DATE = "document_date";
    public static final String DB_COL_PROJECT_PERIOD = "project_period";
    public static final String DB_COL_LAST_UPDATED_TIME = "last_updated_time";
    public static final String DB_COL_APPORVED_BY = "approved_by";

}
