package com.project.backend.features.system.rule.dto;

import java.util.List;

import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.enums.RuleType;
import lombok.Builder;

@Builder
public record RuleMasterResponse(
        Long id,
        String ruleName,
        String ruleDisplayName,
        RuleType ruleType,
        RuleDslType dslType,
        String dslText,
        String ruleBeanName,
        String resultFactKey,
        String description,
        int priority,
        boolean activeFlag,
        List<RuleParameterResponse> parameters,
        List<RuleDataSourceResponse> dataSources
) {
}