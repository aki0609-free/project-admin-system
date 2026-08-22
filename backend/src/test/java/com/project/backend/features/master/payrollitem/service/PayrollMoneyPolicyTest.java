package com.project.backend.features.master.payrollitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class PayrollMoneyPolicyTest {

    private final PayrollMoneyPolicy policy =
            new PayrollMoneyPolicy();

    @Test
    void toDecimal_shouldPreserveExactDecimalRepresentations() {
        assertThat(policy.toDecimal(
                new BigDecimal("1234567890.125"),
                "金額"
        )).isEqualByComparingTo("1234567890.125");
        assertThat(policy.toDecimal(
                new BigInteger("9223372036854775808"),
                "金額"
        )).isEqualByComparingTo("9223372036854775808");
        assertThat(policy.toDecimal(
                "1000.50",
                "金額"
        )).isEqualByComparingTo("1000.50");
    }

    @Test
    void toDecimal_shouldRejectNonFiniteOrNonNumericValues() {
        assertThatThrownBy(() ->
                policy.toDecimal(Double.NaN, "金額"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("有限");
        assertThatThrownBy(() ->
                policy.toDecimal("not-a-number", "金額"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("数値");
    }

    @Test
    void roundToYen_shouldUseHalfUpAtComponentBoundary() {
        assertThat(policy.roundToYen(
                new BigDecimal("100.49")
        )).isEqualByComparingTo("100");
        assertThat(policy.roundToYen(
                new BigDecimal("100.50")
        )).isEqualByComparingTo("101");
    }

    @Test
    void requireNonNegative_shouldRejectNegativeAmount() {
        assertThat(policy.requireNonNegative(
                new BigDecimal("0"),
                "手動変更額"
        )).isZero();

        assertThatThrownBy(() -> policy.requireNonNegative(
                new BigDecimal("-1"),
                "手動変更額"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("手動変更額は0以上で指定してください。");
    }
}
