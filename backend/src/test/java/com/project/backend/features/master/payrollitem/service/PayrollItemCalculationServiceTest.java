package com.project.backend.features.master.payrollitem.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
