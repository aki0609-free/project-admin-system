package com.project.backend.features.master.payrollitem.service;

import java.util.List;
import java.util.Map;

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
        return findItems(
                PayrollItemTargetType.ALLOWANCE,
                parameters
        );
    }

    public List<DailyReportInputItemResponse> findDeductionItems(
            Map<String, Object> parameters
    ) {
        return findItems(
                PayrollItemTargetType.DEDUCTION,
                parameters
        );
    }

    private List<DailyReportInputItemResponse> findItems(
            PayrollItemTargetType targetType,
            Map<String, Object> parameters
    ) {
        return calculationService.calculate(
                        new PayrollItemCalculationRequest(
                                PayrollItemQueryType.DAILY,
                                targetType,
                                parameters
                        )
                )
                .stream()
                .map(this::toDailyInputItem)
                .toList();
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
                .amount(result.amount() == null ? 0 : result.amount().intValue())
                .editable(Boolean.TRUE.equals(result.allowManualInput()))
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
            return DailyReportInputMode.AUTO_CALCULATED;
        }

        if (Boolean.TRUE.equals(allowManualInput)) {
            return DailyReportInputMode.MANUAL;
        }

        return DailyReportInputMode.FIXED;
    }
}