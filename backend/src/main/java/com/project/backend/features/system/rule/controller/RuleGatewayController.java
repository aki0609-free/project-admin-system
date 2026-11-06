package com.project.backend.features.system.rule.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.rule.gateway.RuleGatewayBatchService;
import com.project.backend.features.system.rule.gateway.RuleGatewayExecution;
import com.project.backend.features.system.rule.gateway.RuleGatewayService;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchRequest;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchResponse;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayExecuteRequest;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayExecuteResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/rule-gateway")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class RuleGatewayController {

    private final RuleGatewayService gatewayService;
    private final RuleGatewayBatchService batchService;

    @PostMapping("/execute")
    public RuleGatewayExecuteResponse execute(
            @Valid @RequestBody RuleGatewayExecuteRequest request
    ) {
        RuleGatewayExecution execution = gatewayService.executeFrontend(
                request.gatewayKey(),
                request.targetDate(),
                request.parameters()
        );
        return RuleGatewayExecuteResponse.from(execution);
    }

    @PostMapping("/batch")
    public RuleGatewayBatchResponse executeBatch(
            @Valid @RequestBody RuleGatewayBatchRequest request
    ) {
        return batchService.executeFrontend(request);
    }
}
