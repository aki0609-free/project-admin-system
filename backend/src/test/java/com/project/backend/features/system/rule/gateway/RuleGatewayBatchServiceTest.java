package com.project.backend.features.system.rule.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchCalculationRequest;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchRequest;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchResponse;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchRowRequest;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayFailureMode;
import com.project.backend.features.system.rule.entity.RuleMaster;

class RuleGatewayBatchServiceTest {

    private final RuleGatewayService gatewayService =
            mock(RuleGatewayService.class);
    private final RuleGatewayBatchService service =
            new RuleGatewayBatchService(gatewayService);

    @Test
    void batchReturnsMultipleCalculatedColumnsForEachRow() {
        LocalDate targetDate = LocalDate.of(2026, 11, 5);
        RuleGatewayBinding normal = binding("PAY.NORMAL");
        RuleGatewayBinding overtime = binding("PAY.OVERTIME");
        RuleGatewayPreparedBinding preparedNormal = prepared(normal);
        RuleGatewayPreparedBinding preparedOvertime = prepared(overtime);
        when(gatewayService.prepareFrontendBindings(
                Set.of("PAY.NORMAL", "PAY.OVERTIME"),
                targetDate
        )).thenReturn(Map.of(
                "PAY.NORMAL", preparedNormal,
                "PAY.OVERTIME", preparedOvertime
        ));
        when(gatewayService.requireContract("PAY.NORMAL"))
                .thenReturn(contract("PAY.NORMAL"));
        when(gatewayService.requireContract("PAY.OVERTIME"))
                .thenReturn(contract("PAY.OVERTIME"));
        when(gatewayService.executeFrontendPrepared(
                preparedNormal,
                targetDate,
                Map.of("employeeId", 1, "componentType", "NORMAL")
        )).thenReturn(execution("PAY.NORMAL", 10000));
        when(gatewayService.executeFrontendPrepared(
                preparedOvertime,
                targetDate,
                Map.of("employeeId", 1, "componentType", "OVERTIME")
        )).thenReturn(execution("PAY.OVERTIME", 1250));

        RuleGatewayBatchResponse response = service.executeFrontend(
                new RuleGatewayBatchRequest(
                        targetDate,
                        RuleGatewayFailureMode.FAIL_FAST,
                        List.of(new RuleGatewayBatchRowRequest(
                                "employee-1",
                                Map.of("employeeId", 1)
                        )),
                        List.of(
                                new RuleGatewayBatchCalculationRequest(
                                        "normalAmount",
                                        "PAY.NORMAL",
                                        Map.of("componentType", "NORMAL")
                                ),
                                new RuleGatewayBatchCalculationRequest(
                                        "overtimeAmount",
                                        "PAY.OVERTIME",
                                        Map.of("componentType", "OVERTIME")
                                )
                        )
                )
        );

        assertThat(response.rows()).hasSize(1);
        assertThat(response.bindings())
                .extracting("outputKey")
                .containsExactly("normalAmount", "overtimeAmount");
        assertThat(response.rows().getFirst().values())
                .containsEntry("normalAmount", 10000)
                .containsEntry("overtimeAmount", 1250);
        assertThat(response.rows().getFirst().errors()).isEmpty();
    }

    @Test
    void partialModeKeepsSuccessfulCellsAndReportsFailedCells() {
        LocalDate targetDate = LocalDate.of(2026, 11, 5);
        RuleGatewayBinding normal = binding("PAY.NORMAL");
        RuleGatewayBinding error = binding("PAY.ERROR");
        RuleGatewayPreparedBinding preparedNormal = prepared(normal);
        RuleGatewayPreparedBinding preparedError = prepared(error);
        when(gatewayService.prepareFrontendBindings(
                Set.of("PAY.NORMAL", "PAY.ERROR"),
                targetDate
        )).thenReturn(Map.of(
                "PAY.NORMAL", preparedNormal,
                "PAY.ERROR", preparedError
        ));
        when(gatewayService.requireContract("PAY.NORMAL"))
                .thenReturn(contract("PAY.NORMAL"));
        when(gatewayService.requireContract("PAY.ERROR"))
                .thenReturn(contract("PAY.ERROR"));
        when(gatewayService.executeFrontendPrepared(
                preparedNormal,
                targetDate,
                Map.of("employeeId", 1)
        )).thenReturn(execution("PAY.NORMAL", 10000));
        when(gatewayService.executeFrontendPrepared(
                preparedError,
                targetDate,
                Map.of("employeeId", 1)
        )).thenThrow(new IllegalStateException("sensitive detail"));

        RuleGatewayBatchResponse response = service.executeFrontend(
                new RuleGatewayBatchRequest(
                        targetDate,
                        RuleGatewayFailureMode.PARTIAL,
                        List.of(new RuleGatewayBatchRowRequest(
                                "employee-1",
                                Map.of("employeeId", 1)
                        )),
                        List.of(
                                new RuleGatewayBatchCalculationRequest(
                                        "normalAmount",
                                        "PAY.NORMAL",
                                        Map.of()
                                ),
                                new RuleGatewayBatchCalculationRequest(
                                        "errorAmount",
                                        "PAY.ERROR",
                                        Map.of()
                                )
                        )
                )
        );

        assertThat(response.rows().getFirst().values())
                .containsEntry("normalAmount", 10000);
        assertThat(response.rows().getFirst().errors())
                .containsKey("errorAmount");
        assertThat(response.rows().getFirst().errors()
                .get("errorAmount").message())
                .doesNotContain("sensitive detail");
    }

    private RuleGatewayExecution execution(
            String gatewayKey,
            Object result
    ) {
        return new RuleGatewayExecution(
                gatewayKey,
                "TEST_RULE",
                null,
                RuleGatewayResultShape.SCALAR,
                result,
                Map.of()
        );
    }

    private RuleGatewayBinding binding(String gatewayKey) {
        return new RuleGatewayBinding(gatewayKey, "TEST_RULE", null);
    }

    private RuleGatewayPreparedBinding prepared(
            RuleGatewayBinding binding
    ) {
        RuleMaster rule = new RuleMaster();
        rule.setRuleName(binding.ruleName());
        return new RuleGatewayPreparedBinding(binding, rule);
    }

    private RuleGatewayContract contract(String gatewayKey) {
        return new RuleGatewayContract(
                gatewayKey,
                RuleGatewayResultShape.SCALAR,
                Set.of(),
                true
        );
    }
}
