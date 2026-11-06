package com.project.backend.features.system.rule.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.service.RuleExecutionService;

class RuleGatewayServiceTest {

    private final RuleGatewayContractRegistry contractRegistry =
            mock(RuleGatewayContractRegistry.class);
    private final RuleGatewayBindingResolverRegistry bindingRegistry =
            mock(RuleGatewayBindingResolverRegistry.class);
    private final RuleExecutionService executionService =
            mock(RuleExecutionService.class);
    private final RuleGatewayService service = new RuleGatewayService(
            contractRegistry,
            bindingRegistry,
            executionService,
            new ObjectMapper()
    );

    @BeforeEach
    void setUp() {
        when(contractRegistry.require("PAY.TEST")).thenReturn(
                new RuleGatewayContract(
                        "PAY.TEST",
                        RuleGatewayResultShape.SCALAR,
                        Set.of("amount"),
                        true
                )
        );
    }

    @Test
    void executeResolvesBindingAndReturnsRuleMetadata() {
        LocalDate targetDate = LocalDate.of(2026, 11, 5);
        RuleGatewayBinding binding = new RuleGatewayBinding(
                "PAY.TEST",
                "PAY_RULE",
                3
        );
        when(bindingRegistry.resolve("PAY.TEST", targetDate))
                .thenReturn(binding);
        when(executionService.execute(
                org.mockito.ArgumentMatchers.eq("PAY_RULE"),
                any(RuleContextRequest.class)
        )).thenReturn(RuleExecutionResult.builder()
                .executed(true)
                .result(new BigDecimal("1250"))
                .facts(Map.of("result", new BigDecimal("1250")))
                .build());

        RuleGatewayExecution result = service.executeFrontend(
                "PAY.TEST",
                targetDate,
                Map.of("amount", 1250)
        );

        assertThat(result.gatewayKey()).isEqualTo("PAY.TEST");
        assertThat(result.ruleName()).isEqualTo("PAY_RULE");
        assertThat(result.ruleVersion()).isEqualTo(3);
        assertThat(result.result()).isEqualTo(new BigDecimal("1250"));
        verify(executionService).execute(
                org.mockito.ArgumentMatchers.eq("PAY_RULE"),
                any(RuleContextRequest.class)
        );
    }

    @Test
    void targetDateIsInjectedIntoRuleParameters() {
        LocalDate targetDate = LocalDate.of(2026, 11, 5);
        when(contractRegistry.require("DATE.TEST")).thenReturn(
                new RuleGatewayContract(
                        "DATE.TEST",
                        RuleGatewayResultShape.SCALAR,
                        Set.of("targetDate"),
                        true
                )
        );
        when(bindingRegistry.resolve("DATE.TEST", targetDate)).thenReturn(
                new RuleGatewayBinding("DATE.TEST", "DATE_RULE", null)
        );
        when(executionService.execute(
                org.mockito.ArgumentMatchers.eq("DATE_RULE"),
                any(RuleContextRequest.class)
        )).thenAnswer(invocation -> {
            RuleContextRequest context = invocation.getArgument(1);
            assertThat(context.parameters())
                    .containsEntry("targetDate", targetDate);
            return RuleExecutionResult.builder()
                    .executed(true)
                    .result(1)
                    .build();
        });

        service.executeFrontend("DATE.TEST", targetDate, Map.of());
    }

    @Test
    void trustedBindingParametersOverrideRequestParameters() {
        RuleGatewayBinding binding = new RuleGatewayBinding(
                "PAY.TEST",
                "PAY_RULE",
                null,
                Map.of(
                        "amount", 42,
                        "targetMasterId", 9
                )
        );
        when(executionService.execute(
                org.mockito.ArgumentMatchers.eq("PAY_RULE"),
                any(RuleContextRequest.class)
        )).thenAnswer(invocation -> {
            RuleContextRequest context = invocation.getArgument(1);
            assertThat(context.parameters())
                    .containsEntry("amount", 42)
                    .containsEntry("targetMasterId", 9);
            return RuleExecutionResult.builder()
                    .executed(true)
                    .result(42)
                    .build();
        });

        RuleGatewayExecution result = service.executeBound(
                binding,
                Map.of(
                        "amount", 999,
                        "targetMasterId", 999
                )
        );

        assertThat(result.result()).isEqualTo(42);
    }

    @Test
    void prepareFrontendBindingsLoadsUniqueRulesOnce() {
        LocalDate targetDate = LocalDate.of(2026, 11, 5);
        when(contractRegistry.require("PAY.OTHER")).thenReturn(
                new RuleGatewayContract(
                        "PAY.OTHER",
                        RuleGatewayResultShape.SCALAR,
                        Set.of(),
                        true
                )
        );
        RuleGatewayBinding testBinding = new RuleGatewayBinding(
                "PAY.TEST",
                "SHARED_RULE",
                null
        );
        RuleGatewayBinding otherBinding = new RuleGatewayBinding(
                "PAY.OTHER",
                "SHARED_RULE",
                null
        );
        when(bindingRegistry.resolveAll(
                Set.of("PAY.TEST", "PAY.OTHER"),
                targetDate
        )).thenReturn(Map.of(
                "PAY.TEST", testBinding,
                "PAY.OTHER", otherBinding
        ));
        RuleMaster rule = new RuleMaster();
        rule.setRuleName("SHARED_RULE");
        when(executionService.loadActiveRules(Set.of("SHARED_RULE")))
                .thenReturn(Map.of("SHARED_RULE", rule));

        Map<String, RuleGatewayPreparedBinding> result =
                service.prepareFrontendBindings(
                        Set.of("PAY.TEST", "PAY.OTHER"),
                        targetDate
                );

        assertThat(result).hasSize(2);
        assertThat(result.get("PAY.TEST").rule()).isSameAs(rule);
        assertThat(result.get("PAY.OTHER").rule()).isSameAs(rule);
        verify(executionService).loadActiveRules(Set.of("SHARED_RULE"));
    }

    @Test
    void missingRequiredParameterFailsBeforeRuleExecution() {
        RuleGatewayBinding binding = new RuleGatewayBinding(
                "PAY.TEST",
                "PAY_RULE",
                null
        );

        assertThatThrownBy(() -> service.executeBound(binding, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void frontendExecutionRequiresExplicitContractPermission() {
        when(contractRegistry.require("INTERNAL.TEST")).thenReturn(
                new RuleGatewayContract(
                        "INTERNAL.TEST",
                        RuleGatewayResultShape.SCALAR,
                        Set.of(),
                        false
                )
        );

        assertThatThrownBy(() -> service.executeFrontend(
                "INTERNAL.TEST",
                null,
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("フロント実行");
    }

    @Test
    void scalarContractRejectsCollectionResult() {
        when(executionService.execute(
                org.mockito.ArgumentMatchers.eq("PAY_RULE"),
                any(RuleContextRequest.class)
        )).thenReturn(RuleExecutionResult.builder()
                .executed(true)
                .result(java.util.List.of(1, 2))
                .build());

        assertThatThrownBy(() -> service.executeBound(
                new RuleGatewayBinding("PAY.TEST", "PAY_RULE", null),
                Map.of("amount", 1)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("結果形式");
    }
}
