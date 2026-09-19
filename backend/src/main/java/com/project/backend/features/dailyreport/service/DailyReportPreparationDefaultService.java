package com.project.backend.features.dailyreport.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportPreparationDefaultResponse;
import com.project.backend.features.operation.preparation.entity.DailyPreparation;
import com.project.backend.features.operation.preparation.entity.DailyPreparationAssignment;
import com.project.backend.features.operation.preparation.repository.DailyPreparationAssignmentRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationDispatchRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportPreparationDefaultService {

    private final DailyPreparationRepository preparationRepository;
    private final DailyPreparationAssignmentRepository assignmentRepository;
    private final DailyPreparationDispatchRepository dispatchRepository;
    private final CustomerSiteRepository customerSiteRepository;

    public DailyReportPreparationDefaultResponse find(
            LocalDate workDate,
            Long employeeId
    ) {
        if (workDate == null || employeeId == null) {
            throw new IllegalArgumentException("勤務日と従業員は必須です。");
        }

        DailyPreparation preparation = preparationRepository
                .findByTargetDateAndDeletedAtIsNull(workDate)
                .orElse(null);
        if (preparation == null) {
            return DailyReportPreparationDefaultResponse.unavailable();
        }

        DailyPreparationAssignment assignment = assignmentRepository
                .findByPreparationIdAndEmployeeIdAndDeletedAtIsNull(
                        preparation.getId(),
                        employeeId
                )
                .orElse(null);
        if (assignment == null) {
            return DailyReportPreparationDefaultResponse.unavailable();
        }

        var distance = assignment.getCustomerSiteId() == null
                ? java.math.BigDecimal.ZERO
                : dispatchRepository
                        .findByPreparationIdAndCustomerSiteIdAndDeletedAtIsNull(
                                preparation.getId(),
                                assignment.getCustomerSiteId())
                        .map(dispatch -> java.math.BigDecimal.valueOf(
                                dispatch.getDistanceFromCompanyKm() != null
                                        ? dispatch.getDistanceFromCompanyKm()
                                        : 0))
                        .orElseGet(() -> customerSiteRepository
                                .findByIdAndDeletedAtIsNull(assignment.getCustomerSiteId())
                                .map(site -> java.math.BigDecimal.valueOf(
                                        site.getDistanceFromCompanyKm() != null
                                                ? site.getDistanceFromCompanyKm()
                                                : 0))
                                .orElse(java.math.BigDecimal.ZERO));

        return new DailyReportPreparationDefaultResponse(
                true,
                preparation.getId(),
                assignment.getId(),
                assignment.getCustomerId(),
                assignment.getCustomerSiteId(),
                assignment.getCustomerName(),
                assignment.getSiteName(),
                assignment.getWorkDescription(),
                assignment.getVehicleArrangementType(),
                distance,
                assignment.getPassengerCount()
        );
    }
}
