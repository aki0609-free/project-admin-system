package com.project.backend.features.master.payrollitem.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterDefinition;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterDefinitionRepository;

class SelectOptionCalculationValueResolverTest {

    private PayrollItemParameterDefinitionRepository repository;
    private SelectOptionCalculationValueResolver resolver;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("default");
        repository = mock(PayrollItemParameterDefinitionRepository.class);
        resolver = new SelectOptionCalculationValueResolver(
                repository, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolvesCalculationValueFromSelectedOption() {
        PayrollItemParameterDefinition definition = new PayrollItemParameterDefinition();
        definition.setOptionsJson("""
                [
                  {"label":"一人部屋","value":"SINGLE_ROOM","calculationValue":700},
                  {"label":"複数人部屋","value":"SHARED_ROOM","calculationValue":450}
                ]
                """);
        when(repository
                .findByTenantIdAndBalancePolicyIdAndParameterKeyAndActiveFlagTrueAndDeletedAtIsNull(
                        "default", 10L, "dormitoryType"))
                .thenReturn(Optional.of(definition));

        Object value = resolver.resolve(new PayrollItemRuleParameterResolutionContext(
                1L, LocalDate.of(2026, 8, 22),
                Map.of("dormitoryType", "SHARED_ROOM"),
                10L, "dormitoryType"));

        assertThat(value).isEqualTo(new BigDecimal("450"));
    }

    @Test
    void returnsZeroWhenEmployeeHasNoSelection() {
        Object value = resolver.resolve(new PayrollItemRuleParameterResolutionContext(
                1L, LocalDate.of(2026, 8, 22), Map.of(),
                10L, "dormitoryType"));

        assertThat(value).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsOptionWithoutCalculationValue() {
        PayrollItemParameterDefinition definition = new PayrollItemParameterDefinition();
        definition.setOptionsJson(
                "[{\"label\":\"複数人部屋\",\"value\":\"SHARED_ROOM\"}]"
        );
        when(repository
                .findByTenantIdAndBalancePolicyIdAndParameterKeyAndActiveFlagTrueAndDeletedAtIsNull(
                        "default", 10L, "dormitoryType"))
                .thenReturn(Optional.of(definition));

        assertThatThrownBy(() -> resolver.resolve(
                new PayrollItemRuleParameterResolutionContext(
                        1L, LocalDate.of(2026, 8, 22),
                        Map.of("dormitoryType", "SHARED_ROOM"),
                        10L, "dormitoryType")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("計算値が未設定");
    }
}
