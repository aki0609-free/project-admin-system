package com.project.backend.features.customer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.customer.dto.CustomerDetailResponse;
import com.project.backend.features.customer.dto.CustomerListItemResponse;
import com.project.backend.features.customer.dto.CustomerOptionResponse;
import com.project.backend.features.customer.dto.CustomerSaveRequest;
import com.project.backend.features.customer.service.CustomerCommandService;
import com.project.backend.features.customer.service.CustomerQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerQueryService customerQueryService;
    private final CustomerCommandService customerCommandService;

    @GetMapping
    public List<CustomerListItemResponse> findAll() {
        return customerQueryService.findAll();
    }

    @GetMapping("/{id}")
    public CustomerDetailResponse findDetail(@PathVariable Long id) {
        return customerQueryService.findDetail(id);
    }

    @PostMapping
    public Long create(@RequestBody CustomerSaveRequest request) {
        return customerCommandService.create(request);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable Long id,
            @RequestBody CustomerSaveRequest request
    ) {
        customerCommandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        customerCommandService.delete(id);
    }

    @GetMapping("/options")
    public CustomerOptionResponse findOptions() {
        return customerQueryService.findOptions();
    }
}