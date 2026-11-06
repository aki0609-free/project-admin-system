package com.project.backend.features.system.rule.service.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;

class RuleLoaderTest {

    private final RuleMasterRepository repository = mock(RuleMasterRepository.class);
    private final RuleLoader loader = new RuleLoader(repository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void loadActive_shouldLoadRuleWithinCurrentTenant() {
        TenantContext.setTenantId("tenant-a");
        RuleMaster expected = new RuleMaster();
        when(repository
                .findByTenantIdAndRuleNameAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a",
                        "DAILY_NORMAL_PAY"
                ))
                .thenReturn(Optional.of(expected));

        assertThat(loader.loadActive("DAILY_NORMAL_PAY")).isSameAs(expected);
        verify(repository)
                .findByTenantIdAndRuleNameAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a",
                        "DAILY_NORMAL_PAY"
                );
    }

    @Test
    void loadActive_shouldFailClosedWithoutTenantContext() {
        assertThatThrownBy(() -> loader.loadActive("DAILY_NORMAL_PAY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TenantContextが設定されていません。");
    }

    @Test
    void loadActive_shouldLoadMultipleRulesInOneQuery() {
        TenantContext.setTenantId("tenant-a");
        RuleMaster normal = rule("DAILY_NORMAL_PAY");
        RuleMaster overtime = rule("DAILY_OVERTIME_PAY");
        Set<String> names = Set.of(
                "DAILY_NORMAL_PAY",
                "DAILY_OVERTIME_PAY"
        );
        when(repository
                .findByTenantIdAndRuleNameInAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a",
                        names
                ))
                .thenReturn(List.of(normal, overtime));

        Map<String, RuleMaster> result = loader.loadActive(names);

        assertThat(result)
                .containsEntry("DAILY_NORMAL_PAY", normal)
                .containsEntry("DAILY_OVERTIME_PAY", overtime);
        verify(repository)
                .findByTenantIdAndRuleNameInAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a",
                        names
                );
    }

    private RuleMaster rule(String ruleName) {
        RuleMaster rule = new RuleMaster();
        rule.setRuleName(ruleName);
        return rule;
    }
}
