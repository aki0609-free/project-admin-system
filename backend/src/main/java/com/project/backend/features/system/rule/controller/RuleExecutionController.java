package com.project.backend.features.system.rule.controller;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.rule.dto.RuleExecutionRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.service.RuleExecutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/rules/execution")
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