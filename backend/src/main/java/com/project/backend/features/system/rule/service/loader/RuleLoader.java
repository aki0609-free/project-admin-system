package com.project.backend.features.system.rule.service.loader;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleLoader {

    private final RuleMasterRepository repository;

    public RuleMaster loadActive(String ruleName) {
        return repository.findByRuleNameAndActiveFlagTrueAndDeletedAtIsNull(ruleName)
                .orElseThrow(() ->
                        new RuntimeException("Ruleが見つかりません。 ruleName=" + ruleName)
                );
    }
}