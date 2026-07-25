package com.project.backend.features.master.payrollitem.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.enums.RuleType;
import java.util.Optional;

class PayrollItemMasterValidatorTest {

    private RuleMasterRepository ruleMasterRepository;
    private PayrollItemMasterValidator validator;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
        ruleMasterRepository = mock(RuleMasterRepository.class);
        validator = new PayrollItemMasterValidator(ruleMasterRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void validate_shouldAcceptActiveRuleForAutoCalculation() {
        RuleMaster rule = new RuleMaster();
        rule.setRuleType(RuleType.ALLOWANCE);
        when(ruleMasterRepository
                .findByTenantIdAndRuleNameAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a",
                        "OVERTIME_RULE"
                ))
                .thenReturn(Optional.of(rule));

        validator.validate(
                "OVERTIME",
                "時間外手当",
                "AUTO",
                "OVERTIME_RULE",
                null,
                false,
                0,
                null,
                10,
                RuleType.ALLOWANCE
        );
    }

    @Test
    void validate_shouldRejectInactiveOrMissingRule() {
        assertThatThrownBy(() -> validator.validate(
                "OVERTIME",
                "時間外手当",
                "AUTO",
                "MISSING_RULE",
                null,
                false,
                null,
                null,
                null,
                RuleType.ALLOWANCE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("存在しないか無効");
    }

    @Test
    void validate_shouldRequireAmountForFixedCalculation() {
        assertThatThrownBy(() -> validator.validate(
                "MEAL",
                "食事手当",
                "FIXED",
                null,
                null,
                false,
                null,
                null,
                null,
                RuleType.ALLOWANCE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("初期金額");
    }

    @Test
    void validate_shouldRequireManualInputFlagForManualCalculation() {
        assertThatThrownBy(() -> validator.validate(
                "ADJUSTMENT",
                "調整手当",
                "MANUAL",
                null,
                null,
                false,
                null,
                null,
                null,
                RuleType.ALLOWANCE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("手入力許可");
    }

    @Test
    void normalize_shouldUppercaseCodeAndClearRuleForNonAuto() {
        assertThat(validator.normalizeCode(" meal_allowance "))
                .isEqualTo("MEAL_ALLOWANCE");
        assertThat(validator.normalizeRuleName("FIXED", "SHOULD_CLEAR"))
                .isNull();
    }
}
