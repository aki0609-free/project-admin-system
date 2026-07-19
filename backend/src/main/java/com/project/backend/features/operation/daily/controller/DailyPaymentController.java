package com.project.backend.features.operation.daily.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.operation.daily.dto.DailyPaymentBulkSaveRequest;
import com.project.backend.features.operation.daily.dto.DailyPaymentPrintSummaryResponse;
import com.project.backend.features.operation.daily.dto.DailyPaymentResponse;
import com.project.backend.features.operation.daily.service.DailyPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/operation/daily-payments")
@RequiredArgsConstructor
public class DailyPaymentController {

    private final DailyPaymentService service;

    @GetMapping
    public List<DailyPaymentResponse> findByPaymentDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate
    ) {
        return service.findByPaymentDate(paymentDate);
    }

    @GetMapping("/print-summary")
    public DailyPaymentPrintSummaryResponse getPrintSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate
    ) {
        return service.getPrintSummary(paymentDate);
    }

    @PostMapping("/bulk-save")
    public List<DailyPaymentResponse> bulkSave(
            @Valid @RequestBody DailyPaymentBulkSaveRequest request
    ) {
        return service.bulkSave(request);
    }
}