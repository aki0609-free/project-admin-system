package com.project.backend.features.dailyreport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReportDeduction;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.master.payrollitem.service.PayrollMoneyPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyReportDeductionCommandService {

    private final DailyReportDeductionRepository repository;
    private final PayrollMoneyPolicy moneyPolicy;

    @SuppressWarnings("null")
    public void replaceAll(
            Long dailyReportId,
            List<DailyReportDeductionSaveRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            repository.deleteByDailyReportId(dailyReportId);
            return;
        }

        List<DailyReportDeduction> entities = requests.stream()
                .map(request -> toEntity(dailyReportId, request))
                .toList();

        repository.deleteByDailyReportId(dailyReportId);
        repository.saveAll(entities);
    }

    private DailyReportDeduction toEntity(
            Long dailyReportId,
            DailyReportDeductionSaveRequest request
    ) {
        DailyReportDeduction entity = new DailyReportDeduction();

        entity.setDailyReportId(dailyReportId);
        entity.setDeductionMasterId(request.deductionMasterId());
        entity.setDeductionCode(request.deductionCode());
        entity.setDeductionName(request.deductionName());
        entity.setAmount(nonNegative(
                request.amount(),
                "控除金額"
        ));
        entity.setCalculatedAmount(nonNegative(
                request.calculatedAmount(),
                "控除の計算額",
                entity.getAmount()
        ));
        entity.setManualOverrideFlag(Boolean.TRUE.equals(request.manualOverride()));
        entity.setOverrideReason(entity.isManualOverrideFlag()
                ? normalizeReason(request.overrideReason()) : null);
        entity.setQuantity(request.quantity());
        entity.setBalanceUnit(request.balanceUnit());

        return entity;
    }

    private int nonNegative(Integer value, String valueName) {
        return nonNegative(value, valueName, 0);
    }

    private int nonNegative(
            Integer value,
            String valueName,
            int defaultValue
    ) {
        return moneyPolicy.requireNonNegative(
                java.math.BigDecimal.valueOf(
                        value == null ? defaultValue : value
                ),
                valueName
        ).intValueExact();
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("金額を変更した場合は変更理由が必須です。");
        }
        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("変更理由は500文字以内で入力してください。");
        }
        return normalized;
    }
}
