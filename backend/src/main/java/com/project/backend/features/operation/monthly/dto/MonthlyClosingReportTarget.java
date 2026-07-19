package com.project.backend.features.operation.monthly.dto;

public record MonthlyClosingReportTarget(
        String targetType,
        Long targetId,
        String targetName
) {
    public static MonthlyClosingReportTarget all() {
        return new MonthlyClosingReportTarget(
                "ALL",
                null,
                "全体"
        );
    }

    public static MonthlyClosingReportTarget customer(
            Long customerId,
            String customerName
    ) {
        return new MonthlyClosingReportTarget(
                "CUSTOMER",
                customerId,
                customerName
        );
    }
}