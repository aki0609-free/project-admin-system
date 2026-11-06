package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.entity.DailyPayRuleSetting;
import com.project.backend.features.dailyreport.enums.DailyPayComponentType;
import com.project.backend.features.dailyreport.repository.DailyPayRuleSettingRepository;
import com.project.backend.features.system.rule.gateway.RuleGatewayBinding;

class DailyPayRuleGatewayBindingResolverTest {

    private final DailyPayRuleSettingRepository repository =
            mock(DailyPayRuleSettingRepository.class);
    private final DailyPayRuleGatewayBindingResolver resolver =
            new DailyPayRuleGatewayBindingResolver(repository);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolvesMultipleGatewayKeysWithOneSettingQuery() {
        TenantContext.setTenantId("tenant-a");
        when(repository
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a"
                ))
                .thenReturn(List.of(
                        setting(DailyPayComponentType.NORMAL_PAY, "NORMAL"),
                        setting(
                                DailyPayComponentType.OVERTIME_PAY,
                                "OVERTIME"
                        )
                ));

        Map<String, RuleGatewayBinding> result = resolver.resolveAll(
                Set.of("DAILY_PAY.NORMAL_PAY", "DAILY_PAY.OVERTIME_PAY"),
                LocalDate.of(2026, 11, 5)
        );

        assertThat(result.get("DAILY_PAY.NORMAL_PAY").ruleName())
                .isEqualTo("NORMAL");
        assertThat(result.get("DAILY_PAY.OVERTIME_PAY").ruleName())
                .isEqualTo("OVERTIME");
        verify(repository)
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a"
                );
    }

    private DailyPayRuleSetting setting(
            DailyPayComponentType componentType,
            String ruleName
    ) {
        DailyPayRuleSetting setting = new DailyPayRuleSetting();
        setting.setComponentType(componentType);
        setting.setRuleName(ruleName);
        return setting;
    }
}
