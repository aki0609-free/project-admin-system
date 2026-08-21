package com.project.backend.features.master.payrollitem.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportInputItemResponse;
import com.project.backend.features.dailyreport.enums.DailyReportInputItemType;
import com.project.backend.features.dailyreport.enums.DailyReportInputMode;
import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationRequest;
import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationResult;
import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollItemDailyInputService {

    private final PayrollItemCalculationService calculationService;

    public List<DailyReportInputItemResponse> findAllowanceItems(
            Map<String, Object> parameters
    ) {
        return findAllowanceItems(parameters, Map.of());
    }

    public List<DailyReportInputItemResponse> findAllowanceItems(
            Map<String, Object> parameters,
            Map<Long, Integer> manualAmounts,
            Map<Long, Map<String, Object>> itemParameters,
            Set<Long> excludedMasterIds
    ) {
        return findItems(
                PayrollItemTargetType.ALLOWANCE,
                parameters,
                manualAmounts,
                itemParameters,
                excludedMasterIds
        );
    }

    public List<DailyReportInputItemResponse> findDeductionItems(
            Map<String, Object> parameters
    ) {
        return findDeductionItems(parameters, Map.of());
    }

    public List<DailyReportInputItemResponse> findDeductionItems(
            Map<String, Object> parameters,
            Map<Long, Integer> manualAmounts,
            Map<Long, Map<String, Object>> itemParameters,
            Set<Long> excludedMasterIds
    ) {
        return findItems(
                PayrollItemTargetType.DEDUCTION,
                parameters,
                manualAmounts,
                itemParameters,
                excludedMasterIds
        );
    }

    private List<DailyReportInputItemResponse> findItems(
            PayrollItemTargetType targetType,
            Map<String, Object> parameters,
            Map<Long, Integer> manualAmounts,
            Map<Long, Map<String, Object>> itemParameters,
            Set<Long> excludedMasterIds
    ) {
        return calculationService.calculate(
                        new PayrollItemCalculationRequest(
                                PayrollItemQueryType.DAILY,
                                targetType,
                                parameters,
                                itemParameters,
                                excludedMasterIds
                ),
                        manualAmounts
                )
                .stream()
                .map(this::toDailyInputItem)
                .toList();
    }

    public List<DailyReportInputItemResponse> findAllowanceItems(
            Map<String, Object> parameters,
            Map<Long, Integer> manualAmounts
    ) {
        return findAllowanceItems(parameters, manualAmounts, Map.of(), Set.of());
    }

    public List<DailyReportInputItemResponse> findDeductionItems(
            Map<String, Object> parameters,
            Map<Long, Integer> manualAmounts
    ) {
        return findDeductionItems(parameters, manualAmounts, Map.of(), Set.of());
    }

    private DailyReportInputItemResponse toDailyInputItem(
            PayrollItemCalculationResult result
    ) {
        return DailyReportInputItemResponse.builder()
                .masterId(result.targetMasterId())
                .code(result.targetCode())
                .name(result.targetName())
                .itemType(toInputItemType(result.targetType()))
                .inputMode(resolveInputMode(
                        result.calculationType(),
                        result.allowManualInput()
                ))
                .calculatedAmount(result.calculatedAmount() == null
                        ? 0 : result.calculatedAmount().intValue())
                .amount(result.amount() == null ? 0 : result.amount().intValue())
                .manualOverride(Boolean.TRUE.equals(result.manualOverride()))
                .editable(
                        Boolean.TRUE.equals(result.allowManualInput())
                                && ("MANUAL".equals(result.calculationType())
                                || "AUTO".equals(result.calculationType())
                                || "FIXED".equals(result.calculationType()))
                )
                .displayOrder(result.displayOrder())
                .build();
    }

    private DailyReportInputItemType toInputItemType(
            PayrollItemTargetType targetType
    ) {
        return switch (targetType) {
            case ALLOWANCE -> DailyReportInputItemType.ALLOWANCE;
            case DEDUCTION -> DailyReportInputItemType.DEDUCTION;
        };
    }

    private DailyReportInputMode resolveInputMode(
            String calculationType,
            Boolean allowManualInput
    ) {
        if ("AUTO".equals(calculationType)) {
            return Boolean.TRUE.equals(allowManualInput)
                    ? DailyReportInputMode.AUTO_WITH_OVERRIDE
                    : DailyReportInputMode.AUTO_CALCULATED;
        }

        if ("FIXED".equals(calculationType)) {
            return Boolean.TRUE.equals(allowManualInput)
                    ? DailyReportInputMode.FIXED_WITH_OVERRIDE
                    : DailyReportInputMode.FIXED;
        }

        return Boolean.TRUE.equals(allowManualInput)
                ? DailyReportInputMode.MANUAL
                : DailyReportInputMode.FIXED;
    }
}
