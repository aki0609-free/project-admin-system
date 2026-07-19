package com.project.backend.features.customer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.customer.dto.CustomerSiteBillingRateRequest;
import com.project.backend.features.customer.dto.CustomerSiteBillingRateResponse;
import com.project.backend.features.customer.service.CustomerSiteBillingRateCommandService;
import com.project.backend.features.customer.service.CustomerSiteBillingRateQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers/{customerId}/billing-rates")
@RequiredArgsConstructor
public class CustomerSiteBillingRateController {

        private final CustomerSiteBillingRateQueryService queryService;
        private final CustomerSiteBillingRateCommandService commandService;

        @GetMapping
        public List<CustomerSiteBillingRateResponse> findAll(
                        @PathVariable Long customerId) {
                return queryService.findByCustomerId(customerId);
        }

        @GetMapping("/{billingRateId}")
        public CustomerSiteBillingRateResponse findById(
                        @PathVariable Long customerId,
                        @PathVariable Long billingRateId) {
                return queryService.findByCustomerIdAndId(
                                customerId,
                                billingRateId);
        }

        @PostMapping
        public Long create(
                        @PathVariable Long customerId,
                        @RequestBody CustomerSiteBillingRateRequest request) {
                return commandService.create(customerId, request);
        }

        @PutMapping("/{billingRateId}")
        public void update(
                        @PathVariable Long customerId,
                        @PathVariable Long billingRateId,
                        @RequestBody CustomerSiteBillingRateRequest request) {
                commandService.update(
                                customerId,
                                billingRateId,
                                request);
        }

        @DeleteMapping("/{billingRateId}")
        public void delete(
                        @PathVariable Long customerId,
                        @PathVariable Long billingRateId) {
                commandService.delete(
                                customerId,
                                billingRateId);
        }
}