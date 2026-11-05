package com.project.backend.features.admin.business.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.masterdata.contract.GenericMasterRecordCandidate;

class PayrollClosingSettingGenericMasterContractTest {

    private final PayrollClosingSettingGenericMasterContract contract =
            new PayrollClosingSettingGenericMasterContract();

    @Test
    void validate_shouldAllowExactlyOneValidActiveSetting() {
        assertThatCode(() -> contract.validate(List.of(candidate(
                1L, "END_OF_MONTH", null, 0,
                "DAY_OF_MONTH", 15, 1, true
        )))).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectMissingOrDuplicateActiveSetting() {
        assertThatThrownBy(() -> contract.validate(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1件だけ");
        assertThatThrownBy(() -> contract.validate(List.of(
                candidate(1L, "END_OF_MONTH", null, 0,
                        "DAY_OF_MONTH", 15, 1, true),
                candidate(2L, "DAY_OF_MONTH", 20, 0,
                        "DAY_OF_MONTH", 15, 1, true)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1件だけ");
    }

    @Test
    void validate_shouldRejectInvalidDayAndMonthOffset() {
        assertThatThrownBy(() -> contract.validate(List.of(candidate(
                1L, "DAY_OF_MONTH", 32, 0,
                "DAY_OF_MONTH", 15, 1, true
        )))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1〜31");
        assertThatThrownBy(() -> contract.validate(List.of(candidate(
                1L, "END_OF_MONTH", null, 13,
                "DAY_OF_MONTH", 15, 1, true
        )))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-12〜12");
    }

    private GenericMasterRecordCandidate candidate(
            Long id,
            String closingType,
            Integer closingDay,
            int closingOffset,
            String paymentType,
            Integer paymentDay,
            int paymentOffset,
            boolean active
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("SETTING_CODE", "PAYROLL");
        values.put("CLOSING_DAY_TYPE", closingType);
        values.put("CLOSING_DAY_VALUE", closingDay);
        values.put("CLOSING_MONTH_OFFSET", closingOffset);
        values.put("PAYMENT_DAY_TYPE", paymentType);
        values.put("PAYMENT_DAY_VALUE", paymentDay);
        values.put("PAYMENT_MONTH_OFFSET", paymentOffset);
        return new GenericMasterRecordCandidate(id, values, active);
    }
}
