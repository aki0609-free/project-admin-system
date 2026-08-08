package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyReportSaveValidator {

    private final DailyReportRepository repository;

    public void validateForCreate(DailyReportSaveRequest request) {
        validate(request);
        if (repository.existsByEmployeeIdAndWorkDateAndDeletedAtIsNull(
                request.employeeId(),
                request.workDate()
        )) {
            throw duplicate(request);
        }
    }

    public void validateForUpdate(Long id, DailyReportSaveRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("日報IDは必須です。");
        }
        validate(request);
        if (repository.existsByEmployeeIdAndWorkDateAndIdNotAndDeletedAtIsNull(
                request.employeeId(),
                request.workDate(),
                id
        )) {
            throw duplicate(request);
        }
    }

    private void validate(DailyReportSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("日報リクエストは必須です。");
        }
        if (request.employeeId() == null) {
            throw new IllegalArgumentException("従業員は必須です。");
        }
        if (request.workDate() == null) {
            throw new IllegalArgumentException("勤務日は必須です。");
        }
        if (request.customerSiteId() != null
                && (request.jobCode() == null || request.jobCode().isBlank())) {
            throw new IllegalArgumentException("現場を指定した場合、職種コードは必須です。");
        }
        if (request.breakMinutes() != null
                && (request.breakMinutes() < 0 || request.breakMinutes() > 1_440)) {
            throw new IllegalArgumentException("休憩時間は0〜1440分で指定してください。");
        }
        if (request.dormitoryChargeDays() != null
                && (request.dormitoryChargeDays() < 0
                || request.dormitoryChargeDays() > 31)) {
            throw new IllegalArgumentException("寮費日数は0〜31日で指定してください。");
        }

        nonNegative("通常時間", request.workHours());
        nonNegative("残業時間", request.overtimeHours());
        nonNegative("深夜時間", request.nightWorkHours());
        nonNegative("休日時間", request.holidayWorkHours());
        nonNegative("貸付返済額", request.loanRepaymentAmount());
        nonNegative("積立額", request.savingAmount());
        nonNegative("走行距離", request.mileage());
        nonNegative("有給日数", request.paidLeaveDays());

    }

    private void nonNegative(String label, BigDecimal value) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(label + "は0以上で指定してください。");
        }
    }

    private IllegalArgumentException duplicate(DailyReportSaveRequest request) {
        return new IllegalArgumentException(
                "同じ従業員・勤務日の日報が既に登録されています。employeeId="
                        + request.employeeId()
                        + ", workDate="
                        + request.workDate()
        );
    }
}
