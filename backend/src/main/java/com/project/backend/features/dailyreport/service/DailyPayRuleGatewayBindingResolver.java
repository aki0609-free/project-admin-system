package com.project.backend.features.dailyreport.service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.enums.DailyPayComponentType;
import com.project.backend.features.dailyreport.repository.DailyPayRuleSettingRepository;
import com.project.backend.features.system.rule.gateway.RuleGatewayBinding;
import com.project.backend.features.system.rule.gateway.RuleGatewayBindingResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyPayRuleGatewayBindingResolver
        implements RuleGatewayBindingResolver {

    private final DailyPayRuleSettingRepository repository;

    @Override
    public boolean supports(String gatewayKey) {
        return DailyPayRuleGatewayKeys.supports(gatewayKey);
    }

    @Override
    public RuleGatewayBinding resolve(
            String gatewayKey,
            LocalDate targetDate
    ) {
        return resolveAll(Set.of(gatewayKey), targetDate).get(gatewayKey);
    }

    @Override
    public Map<String, RuleGatewayBinding> resolveAll(
            Set<String> gatewayKeys,
            LocalDate targetDate
    ) {
        Map<DailyPayComponentType, String> ruleNames =
                new EnumMap<>(DailyPayComponentType.class);
        repository.findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                TenantContext.getTenantId()
        ).forEach(setting -> {
            String previous = ruleNames.putIfAbsent(
                    setting.getComponentType(),
                    setting.getRuleName()
            );
            if (previous != null) {
                throw new IllegalStateException(
                        "日報給与Rule設定が重複しています。componentType="
                                + setting.getComponentType()
                );
            }
        });

        Map<String, RuleGatewayBinding> result = new LinkedHashMap<>();
        for (String gatewayKey : gatewayKeys) {
            DailyPayComponentType componentType =
                    DailyPayRuleGatewayKeys.toComponent(gatewayKey);
            String ruleName = ruleNames.get(componentType);
            if (ruleName == null || ruleName.isBlank()) {
                throw new IllegalStateException(
                        "必須の日報給与Rule設定がありません。componentType="
                                + componentType
                );
            }
            result.put(
                    gatewayKey,
                    new RuleGatewayBinding(gatewayKey, ruleName, null)
            );
        }
        return Map.copyOf(result);
    }
}
