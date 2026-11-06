package com.project.backend.features.system.rule.service.loader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleLoader {

    private final RuleMasterRepository repository;

    public RuleMaster loadActive(String ruleName) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("TenantContextが設定されていません。");
        }

        return repository
                .findByTenantIdAndRuleNameAndActiveFlagTrueAndDeletedAtIsNull(
                        tenantId,
                        ruleName
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Ruleが見つかりません。 tenantId="
                                        + tenantId
                                        + ", ruleName="
                                        + ruleName
                        )
                );
    }

    public Map<String, RuleMaster> loadActive(Set<String> ruleNames) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("TenantContextが設定されていません。");
        }

        Map<String, RuleMaster> result = new LinkedHashMap<>();
        repository
                .findByTenantIdAndRuleNameInAndActiveFlagTrueAndDeletedAtIsNull(
                        tenantId,
                        ruleNames
                )
                .forEach(rule -> result.put(rule.getRuleName(), rule));
        for (String ruleName : ruleNames) {
            if (!result.containsKey(ruleName)) {
                throw new EntityNotFoundException(
                        "Ruleが見つかりません。 tenantId="
                                + tenantId
                                + ", ruleName="
                                + ruleName
                );
            }
        }
        return Map.copyOf(result);
    }
}
