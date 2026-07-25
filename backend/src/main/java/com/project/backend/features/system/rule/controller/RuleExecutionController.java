package com.project.backend.features.system.rule.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.project.backend.features.system.rule.dto.RuleExecutionRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.service.RuleExecutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/rules/execution")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class RuleExecutionController {

    private final RuleExecutionService service;

    @PostMapping("/fire")
    public RuleExecutionResult fire(
            @RequestBody RuleExecutionRequest request
    ) {
        return service.execute(
                request.ruleName(),
                request.context()
        );
    }
}
