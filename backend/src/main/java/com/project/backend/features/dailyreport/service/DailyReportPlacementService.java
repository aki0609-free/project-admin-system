package com.project.backend.features.dailyreport.service;

import org.springframework.stereotype.Service;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.operation.preparation.repository.DailyPreparationAssignmentRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 日報の顧客・現場を「現場配置・配車」の確定内容へ統一する。
 */
@Service
@RequiredArgsConstructor
public class DailyReportPlacementService {

    private final DailyPreparationRepository preparationRepository;
    private final DailyPreparationAssignmentRepository assignmentRepository;

    public void applyAuthoritativePlacement(DailyReport report) {
        var preparation = preparationRepository
                .findByTargetDateAndDeletedAtIsNull(report.getWorkDate())
                .orElseThrow(() -> new IllegalArgumentException(
                        "勤務日の現場配置・配車が登録されていません。先に現場配置・配車を登録してください。"));

        var assignment = assignmentRepository
                .findByPreparationIdAndEmployeeIdAndDeletedAtIsNull(
                        preparation.getId(), report.getEmployee().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "対象従業員の現場配置が登録されていません。現場配置・配車を修正してください。"));

        report.setCustomerId(assignment.getCustomerId());
        report.setCustomerSiteId(assignment.getCustomerSiteId());
        report.setCustomerName(assignment.getCustomerName());
        report.setSiteName(assignment.getSiteName());
    }
}
