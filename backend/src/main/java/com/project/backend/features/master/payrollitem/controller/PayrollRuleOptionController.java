package com.project.backend.features.master.payrollitem.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.backend.features.master.payrollitem.dto.PayrollRuleOptionResponse;
import com.project.backend.features.master.payrollitem.service.PayrollRuleOptionService;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/master/payroll-rule-options")
@PreAuthorize("hasAuthority('master:manage')")
@RequiredArgsConstructor
public class PayrollRuleOptionController {

    private final PayrollRuleOptionService service;

    @GetMapping
    public List<PayrollRuleOptionResponse> findActiveRules(
            @RequestParam PayrollItemTargetType targetType
    ) {
        return service.findActiveRules(targetType);
    }
}
