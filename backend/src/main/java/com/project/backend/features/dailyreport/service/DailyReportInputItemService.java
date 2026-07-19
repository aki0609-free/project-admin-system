package com.project.backend.features.dailyreport.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.context.DailyReportCalculationContext;
import com.project.backend.features.dailyreport.dto.DailyReportInputResponse;
import com.project.backend.features.master.payrollitem.service.PayrollItemDailyInputService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportInputItemService {

    private final PayrollItemDailyInputService payrollItemDailyInputService;

    public DailyReportInputResponse findItems() {
        return findItems(
                DailyReportCalculationContext.builder()
                        .build()
        );
    }

    public DailyReportInputResponse findItems(
            DailyReportCalculationContext context
    ) {
        return DailyReportInputResponse.builder()
                .allowances(
                        payrollItemDailyInputService.findAllowanceItems(
                                context.toParameters()
                        )
                )
                .deductions(
                        payrollItemDailyInputService.findDeductionItems(
                                context.toParameters()
                        )
                )
                .build();
    }
}