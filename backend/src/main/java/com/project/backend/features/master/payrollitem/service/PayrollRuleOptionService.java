package com.project.backend.features.master.payrollitem.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.payrollitem.dto.PayrollRuleOptionResponse;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.system.rule.enums.RuleType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollRuleOptionService {

    private final RuleMasterRepository ruleMasterRepository;

    public List<PayrollRuleOptionResponse> findActiveRules(
            PayrollItemTargetType targetType
    ) {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("TenantContext に tenantId が設定されていません。");
        }

        if (targetType == null) {
            throw new IllegalArgumentException("targetType は必須です。");
        }

        RuleType ruleType = switch (targetType) {
            case ALLOWANCE -> RuleType.ALLOWANCE;
            case DEDUCTION -> RuleType.DEDUCTION;
        };

        return ruleMasterRepository
                .findByTenantIdAndRuleTypeInAndActiveFlagTrueAndDeletedAtIsNullOrderByRuleNameAsc(
                        tenantId,
                        List.of(ruleType, RuleType.GENERAL)
                )
                .stream()
                .map(rule -> new PayrollRuleOptionResponse(
                        rule.getId(),
                        rule.getRuleName(),
                        rule.getRuleDisplayName()
                ))
                .toList();
    }
}
