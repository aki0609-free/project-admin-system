package com.project.backend.features.operation.monthly.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.operation.monthly.dto.CustomerBillingClosingResponse;
import com.project.backend.features.operation.monthly.dto.CustomerBillingBulkClosingResponse;
import com.project.backend.features.operation.monthly.dto.CustomerBillingSummaryResponse;
import com.project.backend.features.operation.monthly.service.CustomerBillingClosingCommandService;
import com.project.backend.features.operation.monthly.service.CustomerBillingSummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/operation/customer-billing")
@RequiredArgsConstructor
public class CustomerBillingClosingController {

    private final CustomerBillingSummaryService summaryService;
    private final CustomerBillingClosingCommandService commandService;

    @GetMapping("/summary")
    public CustomerBillingSummaryResponse findSummary(
            @RequestParam String targetMonth
    ) {
        return summaryService.findSummary(targetMonth);
    }

    @PostMapping("/close")
    public CustomerBillingClosingResponse close(
            @RequestParam String targetMonth,
            @RequestParam Long customerId
    ) {
        return commandService.close(targetMonth, customerId);
    }

    @PostMapping("/reclose")
    public CustomerBillingClosingResponse reclose(
            @RequestParam String targetMonth,
            @RequestParam Long customerId
    ) {
        return commandService.reclose(targetMonth, customerId);
    }

    @PostMapping("/close-all")
    public CustomerBillingBulkClosingResponse closeAll(
            @RequestParam String targetMonth
    ) {
        return commandService.closeAllEligible(targetMonth);
    }
}
