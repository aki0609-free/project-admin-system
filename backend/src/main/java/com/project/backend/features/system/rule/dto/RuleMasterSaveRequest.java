package com.project.backend.features.system.rule.dto;

import java.util.List;

import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.enums.RuleType;

public record RuleMasterSaveRequest(
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
        List<RuleParameterSaveRequest> parameters,
        List<RuleDataSourceSaveRequest> dataSources
) {
}