package com.project.backend.features.master.payrollitem.enums;

public enum PayrollItemInputSource {
    DAILY_REPORT,
    TRANSACTION,
    DAILY_REPORT_AND_TRANSACTION;

    public boolean supportsDailyReport() {
        return this == DAILY_REPORT || this == DAILY_REPORT_AND_TRANSACTION;
    }

    public boolean supportsTransaction() {
        return this == TRANSACTION || this == DAILY_REPORT_AND_TRANSACTION;
    }
}
