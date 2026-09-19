package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;
import com.project.backend.features.dailyreport.enums.VehicleArrangementType;

public record DailyReportPreparationDefaultResponse(
        boolean available,
        Long preparationId,
        Long assignmentId,
        Long customerId,
        Long customerSiteId,
        String customerName,
        String siteName,
        String workDescription,
        VehicleArrangementType vehicleArrangementType,
        BigDecimal mileage,
        Integer passengerCount
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
                null,
                VehicleArrangementType.NONE,
                BigDecimal.ZERO,
                0
        );
    }
}
