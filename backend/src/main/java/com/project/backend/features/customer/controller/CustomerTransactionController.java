package com.project.backend.features.customer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.customer.dto.CustomerPaymentConfirmRequest;
import com.project.backend.features.customer.dto.CustomerTransactionClosingRequest;
import com.project.backend.features.customer.dto.CustomerTransactionRequest;
import com.project.backend.features.customer.dto.CustomerTransactionResponse;
import com.project.backend.features.customer.service.CustomerTransactionCommandService;
import com.project.backend.features.customer.service.CustomerTransactionQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CustomerTransactionController {

    private final CustomerTransactionQueryService queryService;
    private final CustomerTransactionCommandService commandService;

    @GetMapping("/api/customer-transactions")
    public List<CustomerTransactionResponse> findAll(
            @RequestParam(required = false) Long customerId
    ) {
        return queryService.findAll(customerId);
    }

    @GetMapping("/api/customers/{customerId}/transactions")
    public List<CustomerTransactionResponse> findByCustomerId(
            @PathVariable Long customerId
    ) {
        return queryService.findByCustomerId(customerId);
    }

    @PostMapping("/api/customer-transactions/from-monthly-closing")
    public Long upsertFromMonthlyClosing(
            @RequestBody CustomerTransactionClosingRequest request
    ) {
        return commandService.upsertFromMonthlyClosing(request);
    }

    @PostMapping("/api/customers/{customerId}/transactions")
    public Long create(
            @PathVariable Long customerId,
            @RequestBody CustomerTransactionRequest request
    ) {
        return commandService.create(customerId, request);
    }

    @PutMapping("/api/customers/{customerId}/transactions/{transactionId}")
    public void update(
            @PathVariable Long customerId,
            @PathVariable Long transactionId,
            @RequestBody CustomerTransactionRequest request
    ) {
        commandService.update(customerId, transactionId, request);
    }

    @PutMapping("/api/customers/{customerId}/transactions/{transactionId}/confirm-payment")
    public void confirmPayment(
            @PathVariable Long customerId,
            @PathVariable Long transactionId,
            @RequestBody CustomerPaymentConfirmRequest request
    ) {
        commandService.confirmPayment(customerId, transactionId, request);
    }

    @DeleteMapping("/api/customers/{customerId}/transactions/{transactionId}")
    public void delete(
            @PathVariable Long customerId,
            @PathVariable Long transactionId
    ) {
        commandService.delete(customerId, transactionId);
    }
}