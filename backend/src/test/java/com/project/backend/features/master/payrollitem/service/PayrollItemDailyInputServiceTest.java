package com.project.backend.features.master.payrollitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.dailyreport.enums.DailyReportInputMode;
import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationRequest;
import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationResult;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

class PayrollItemDailyInputServiceTest {

    @Test
    void findAllowanceItems_shouldAllowOverrideWhenAutoMasterPermitsManualInput() {
        PayrollItemCalculationService calculationService =
                mock(PayrollItemCalculationService.class);
        PayrollItemDailyInputService service =
                new PayrollItemDailyInputService(calculationService);

        when(calculationService.calculate(
                org.mockito.ArgumentMatchers.any(PayrollItemCalculationRequest.class),
                org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(List.of(
                result(1L, "AUTO", true),
                result(2L, "FIXED", true),
                result(3L, "MANUAL", true)
        ));

        var items = service.findAllowanceItems(Map.of(), Map.of());

        assertThat(items).extracting(item -> item.inputMode())
                .containsExactly(
                        DailyReportInputMode.AUTO_WITH_OVERRIDE,
                        DailyReportInputMode.FIXED_WITH_OVERRIDE,
                        DailyReportInputMode.MANUAL
                );
        assertThat(items).extracting(item -> item.editable())
                .containsExactly(true, true, true);
    }

    private PayrollItemCalculationResult result(
            Long id,
            String calculationType,
            Boolean allowManualInput
    ) {
        return new PayrollItemCalculationResult(
                PayrollItemTargetType.ALLOWANCE,
                id,
                "ITEM_" + id,
                "項目" + id,
                calculationType,
                "AUTO".equals(calculationType) ? "RULE_" + id : null,
                BigDecimal.valueOf(100),
                allowManualInput,
                id.intValue(),
                Map.of()
        );
    }
}
