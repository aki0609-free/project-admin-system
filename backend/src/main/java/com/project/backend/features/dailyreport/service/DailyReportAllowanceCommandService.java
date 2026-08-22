package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportAllowanceSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReportAllowance;
import com.project.backend.features.dailyreport.repository.DailyReportAllowanceRepository;
import com.project.backend.features.master.payrollitem.service.PayrollMoneyPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyReportAllowanceCommandService {

    private final DailyReportAllowanceRepository repository;
    private final PayrollMoneyPolicy moneyPolicy;

    @SuppressWarnings("null")
    public void replaceAll(
            Long dailyReportId,
            List<DailyReportAllowanceSaveRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            repository.deleteByDailyReportId(dailyReportId);
            return;
        }

        List<DailyReportAllowance> entities = requests.stream()
                .map(request -> toEntity(dailyReportId, request))
                .toList();

        repository.deleteByDailyReportId(dailyReportId);
        repository.saveAll(entities);
    }

    private DailyReportAllowance toEntity(
            Long dailyReportId,
            DailyReportAllowanceSaveRequest request
    ) {
        DailyReportAllowance entity = new DailyReportAllowance();

        entity.setDailyReportId(dailyReportId);
        entity.setAllowanceMasterId(request.allowanceMasterId());
        entity.setAllowanceCode(request.allowanceCode());
        entity.setAllowanceName(request.allowanceName());
        entity.setAmount(nonNegative(
                request.amount(),
                "手当金額"
        ));
        entity.setCalculatedAmount(nonNegative(
                request.calculatedAmount(),
                "手当の計算額",
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
                BigDecimal.valueOf(value == null ? defaultValue : value),
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
