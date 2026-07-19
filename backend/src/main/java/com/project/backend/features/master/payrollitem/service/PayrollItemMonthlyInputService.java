package com.project.backend.features.master.payrollitem.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationRequest;
import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationResult;
import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollItemMonthlyInputService {

    private final PayrollItemCalculationService calculationService;

    public List<PayrollItemCalculationResult> findAllowanceItems(
            Map<String, Object> parameters
    ) {
        return calculationService.calculate(
                new PayrollItemCalculationRequest(
                        PayrollItemQueryType.MONTHLY,
                        PayrollItemTargetType.ALLOWANCE,
                        parameters
                )
        );
    }

    public List<PayrollItemCalculationResult> findDeductionItems(
            Map<String, Object> parameters
    ) {
        return calculationService.calculate(
                new PayrollItemCalculationRequest(
                        PayrollItemQueryType.MONTHLY,
                        PayrollItemTargetType.DEDUCTION,
                        parameters
                )
        );
    }

    public List<PayrollItemCalculationResult> findAllItems(
            Map<String, Object> parameters
    ) {
        return calculationService.calculate(
                new PayrollItemCalculationRequest(
                        PayrollItemQueryType.MONTHLY,
                        null,
                        parameters
                )
        );
    }
}