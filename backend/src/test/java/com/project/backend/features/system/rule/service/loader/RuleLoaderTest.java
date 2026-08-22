package com.project.backend.features.system.rule.service.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
}
