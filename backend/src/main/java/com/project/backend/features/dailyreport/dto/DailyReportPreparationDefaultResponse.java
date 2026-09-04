package com.project.backend.features.dailyreport.dto;

public record DailyReportPreparationDefaultResponse(
        boolean available,
        Long preparationId,
        Long assignmentId,
        Long customerId,
        Long customerSiteId,
        String customerName,
        String siteName,
        String workDescription
) {

    public static DailyReportPreparationDefaultResponse unavailable() {
        return new DailyReportPreparationDefaultResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
