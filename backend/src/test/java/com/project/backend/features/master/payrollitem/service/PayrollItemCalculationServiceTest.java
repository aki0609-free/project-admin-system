package com.project.backend.features.master.payrollitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationRequest;
import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueResult;
import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

class PayrollItemCalculationServiceTest {

    private final PayrollItemQueryService queryService =
            mock(PayrollItemQueryService.class);
    private final PayrollItemValueService valueService =
            mock(PayrollItemValueService.class);
    private final PayrollItemCalculationService service =
            new PayrollItemCalculationService(
                    queryService,
                    valueService,
                    new PayrollMoneyPolicy()
            );

    @Test
    void calculate_shouldKeepManualItemAmountWithoutTreatingItAsOverride() {
        PayrollItemMasterSnapshot master = new PayrollItemMasterSnapshot(
                PayrollItemTargetType.DEDUCTION,
                10L,
                "MOBILE_RENTAL",
                "携帯電話貸出料",
                "MANUAL",
                null,
                0,
                0,
                null,
                true,
                1
        );
        when(queryService.findItems(
                PayrollItemQueryType.DAILY,
                PayrollItemTargetType.DEDUCTION
        )).thenReturn(List.of(master));
        when(valueService.calculate(any())).thenAnswer(invocation -> {
            var valueRequest = invocation.getArgument(
                    0,
                    com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest.class
            );
            return new PayrollItemValueResult(
                    PayrollItemTargetType.DEDUCTION,
                    10L,
                    "MOBILE_RENTAL",
                    "携帯電話貸出料",
                    "MANUAL",
                    null,
                    BigDecimal.valueOf(valueRequest.manualAmount()),
                    Map.of()
            );
        });

        PayrollItemCalculationRequest request = new PayrollItemCalculationRequest(
                PayrollItemQueryType.DAILY,
                PayrollItemTargetType.DEDUCTION,
                Map.of()
        );

        var result = service.calculate(request, Map.of(10L, 1_000), Set.of());

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.amount()).isEqualByComparingTo("1000");
            assertThat(item.manualOverride()).isFalse();
        });
        var requestCaptor = ArgumentCaptor.forClass(
                com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest.class
        );
        verify(valueService).calculate(requestCaptor.capture());
        assertThat(requestCaptor.getValue().manualAmount()).isEqualTo(1_000);
    }

    @Test
    void calculate_shouldUseRuleResultForDailyReserveUntilManuallyOverridden() {
        PayrollItemMasterSnapshot master = new PayrollItemMasterSnapshot(
                PayrollItemTargetType.DEDUCTION,
                13L,
                "LEGAL_DEPOSIT",
                "法定準備金",
                "AUTO",
                "DAILY_LEGAL_DEPOSIT_ESTIMATE",
                0,
                0,
                10_000_000,
                true,
                160
        );
        when(queryService.findItems(
                PayrollItemQueryType.DAILY,
                PayrollItemTargetType.DEDUCTION
        )).thenReturn(List.of(master));
        when(valueService.calculate(any())).thenAnswer(invocation -> {
            var valueRequest = invocation.getArgument(
                    0,
                    com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest.class
            );
            return new PayrollItemValueResult(
                    PayrollItemTargetType.DEDUCTION,
                    13L,
                    "LEGAL_DEPOSIT",
                    "法定準備金",
                    "AUTO",
                    "DAILY_LEGAL_DEPOSIT_ESTIMATE",
                    BigDecimal.valueOf(2_000),
                    Map.of()
            );
        });

        var result = service.calculate(
                new PayrollItemCalculationRequest(
                        PayrollItemQueryType.DAILY,
                        PayrollItemTargetType.DEDUCTION,
                        Map.of()
                ),
                Map.of(),
                Set.of()
        );

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.amount()).isEqualByComparingTo("2000");
            assertThat(item.manualOverride()).isFalse();
            assertThat(item.allowManualInput()).isTrue();
        });
    }

    @Test
    void calculate_shouldRejectNegativeManualOverrideBeforeLimitClamping() {
        PayrollItemMasterSnapshot master = new PayrollItemMasterSnapshot(
                PayrollItemTargetType.DEDUCTION,
                10L,
                "TEST_DEDUCTION",
                "テスト控除",
                "AUTO",
                "TEST_RULE",
                0,
                0,
                10_000,
                true,
                1
        );
        when(queryService.findItems(
                PayrollItemQueryType.DAILY,
                PayrollItemTargetType.DEDUCTION
        )).thenReturn(List.of(master));
        when(valueService.calculate(
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new PayrollItemValueResult(
                PayrollItemTargetType.DEDUCTION,
                10L,
                "TEST_DEDUCTION",
                "テスト控除",
                "AUTO",
                "TEST_RULE",
                BigDecimal.valueOf(500),
                Map.of()
        ));

        PayrollItemCalculationRequest request =
                new PayrollItemCalculationRequest(
                        PayrollItemQueryType.DAILY,
                        PayrollItemTargetType.DEDUCTION,
                        Map.of()
                );

        assertThatThrownBy(() -> service.calculate(
                request,
                Map.of(10L, -1)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("給与項目の手動変更額は0以上で指定してください。");
    }
}
