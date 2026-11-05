package com.project.backend.features.admin.business.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.masterdata.contract.GenericMasterRecordCandidate;

class PayrollPolicyGenericMasterContractTest {

    private final PayrollPolicyGenericMasterContract contract =
            new PayrollPolicyGenericMasterContract();

    @Test
    void validate_shouldRejectOverlappingActivePeriods() {
        assertThatThrownBy(() -> contract.validate(List.of(
                candidate(1L, "2026-01-01", "2026-12-31", true),
                candidate(2L, "2026-10-01", null, true)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("適用期間が既存設定と重複");
    }

    @Test
    void validate_shouldAllowAdjacentOrInactivePeriods() {
        assertThatCode(() -> contract.validate(List.of(
                candidate(1L, "2026-01-01", "2026-09-30", true),
                candidate(2L, "2026-10-01", null, true),
                candidate(3L, "2026-05-01", null, false)
        ))).doesNotThrowAnyException();
    }

    private GenericMasterRecordCandidate candidate(
            Long id,
            String from,
            String to,
            boolean active
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("EFFECTIVE_FROM", from);
        values.put("EFFECTIVE_TO", to);
        values.put("WEEK_START_DAY", "MONDAY");
        values.put("WEEKLY_STATUTORY_HOURS", 40);
        values.put("MONTHLY_OVERTIME_THRESHOLD_HOURS", 60);
        values.put("OVERTIME_RATE", 1.25);
        values.put("OVERTIME_OVER_THRESHOLD_RATE", 1.5);
        values.put("NIGHT_PREMIUM_RATE", 0.25);
        values.put("STATUTORY_HOLIDAY_RATE", 1.35);
        values.put("DAILY_STANDARD_HOURS", 8);
        values.put("AMOUNT_ROUNDING_MODE", "HALF_UP");
        return new GenericMasterRecordCandidate(id, values, active);
    }
}
