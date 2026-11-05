package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.masterdata.contract.GenericMasterRecordCandidate;

class ResignationChecklistGenericMasterContractTest {

    private final ResignationChecklistGenericMasterContract contract =
            new ResignationChecklistGenericMasterContract();

    @Test
    void validate_shouldRejectDuplicateActiveCodesIgnoringCase() {
        assertThatThrownBy(() -> contract.validate(List.of(
                candidate(1L, "RETURN_CARD", 10, true),
                candidate(2L, "return_card", 20, true)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TODOコード");
    }

    @Test
    void validate_shouldAllowUniqueOrInactiveDuplicateCodes() {
        assertThatCode(() -> contract.validate(List.of(
                candidate(1L, "RETURN_CARD", 10, true),
                candidate(2L, "RETURN_CARD", 20, false),
                candidate(3L, "RETURN_EQUIPMENT", 30, true)
        ))).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectNegativeOrFractionalDisplayOrder() {
        assertThatThrownBy(() -> contract.validate(List.of(
                candidate(1L, "RETURN_CARD", -1, true)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0以上の整数");

        assertThatThrownBy(() -> contract.validate(List.of(
                candidate(1L, "RETURN_CARD", 1.5, true)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0以上の整数");
    }

    private GenericMasterRecordCandidate candidate(
            Long id,
            String code,
            Number displayOrder,
            boolean active
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("CODE", code);
        values.put("NAME", "確認項目");
        values.put("DESCRIPTION", null);
        values.put("REQUIRED_FLAG", true);
        values.put("DISPLAY_ORDER", displayOrder);
        return new GenericMasterRecordCandidate(id, values, active);
    }
}
