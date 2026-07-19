package com.project.backend.common.dayrule.utils;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.dto.DayRuleResponse;
import com.project.backend.common.dayrule.enums.DayRuleType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DayRuleUtils の単体テスト。
 *
 * DayRule は「15日」「末日」「31日指定だが2月なら月末扱い」
 * のような業務日付ルールを共通的に扱うための基盤。
 *
 * Customer の締め日・支払日、契約更新日、通知基準日などで使うため、
 * CIで日付解決ロジックを固定しておく。
 */
class DayRuleUtilsTest {

    /**
     * DAY_OF_MONTH の通常ケース。
     *
     * 15日指定なら、その年月の15日に解決されることを確認する。
     */
    @Test
    void resolve_shouldReturnSpecifiedDay_whenDayOfMonth() {
        LocalDate result = DayRuleUtils.resolve(
                DayRuleType.DAY_OF_MONTH,
                15,
                YearMonth.of(2026, 5)
        );

        assertThat(result).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    /**
     * END_OF_MONTH の通常ケース。
     *
     * 末日指定なら、その年月の最終日に解決されることを確認する。
     */
    @Test
    void resolve_shouldReturnEndOfMonth_whenEndOfMonth() {
        LocalDate result = DayRuleUtils.resolve(
                DayRuleType.END_OF_MONTH,
                null,
                YearMonth.of(2026, 5)
        );

        assertThat(result).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    /**
     * 31日指定で、対象月が30日までしかない場合。
     *
     * 4月31日は存在しないため、4月30日に丸める。
     */
    @Test
    void resolve_shouldReturnLastDay_whenSpecifiedDayExceedsMonthLength() {
        LocalDate result = DayRuleUtils.resolve(
                DayRuleType.DAY_OF_MONTH,
                31,
                YearMonth.of(2026, 4)
        );

        assertThat(result).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    /**
     * 31日指定で、対象月が平年2月の場合。
     *
     * 2月31日は存在しないため、2月28日に丸める。
     */
    @Test
    void resolve_shouldReturnFeb28_whenSpecified31InNonLeapYearFebruary() {
        LocalDate result = DayRuleUtils.resolve(
                DayRuleType.DAY_OF_MONTH,
                31,
                YearMonth.of(2026, 2)
        );

        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    /**
     * 31日指定で、対象月がうるう年2月の場合。
     *
     * 2月31日は存在しないため、うるう年の月末である2月29日に丸める。
     */
    @Test
    void resolve_shouldReturnFeb29_whenSpecified31InLeapYearFebruary() {
        LocalDate result = DayRuleUtils.resolve(
                DayRuleType.DAY_OF_MONTH,
                31,
                YearMonth.of(2024, 2)
        );

        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    /**
     * type が null の場合。
     *
     * ルール未設定として null を返す。
     */
    @Test
    void resolve_shouldReturnNull_whenTypeIsNull() {
        LocalDate result = DayRuleUtils.resolve(
                null,
                15,
                YearMonth.of(2026, 5)
        );

        assertThat(result).isNull();
    }

    /**
     * DAY_OF_MONTH で value が null の場合。
     *
     * 日付値がないため解決不可として null を返す。
     */
    @Test
    void resolve_shouldReturnNull_whenDayOfMonthValueIsNull() {
        LocalDate result = DayRuleUtils.resolve(
                DayRuleType.DAY_OF_MONTH,
                null,
                YearMonth.of(2026, 5)
        );

        assertThat(result).isNull();
    }

    /**
     * DayRule record を渡して日付解決できることを確認する。
     */
    @Test
    void resolve_shouldAcceptDayRuleRecord() {
        DayRule rule = DayRule.builder()
                .type(DayRuleType.DAY_OF_MONTH)
                .value(20)
                .build();

        LocalDate result = DayRuleUtils.resolve(
                rule,
                YearMonth.of(2026, 5)
        );

        assertThat(result).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    /**
     * DAY_OF_MONTH の表示ラベルを確認する。
     */
    @Test
    void toLabel_shouldReturnDayLabel_whenDayOfMonth() {
        String label = DayRuleUtils.toLabel(
                DayRuleType.DAY_OF_MONTH,
                25
        );

        assertThat(label).isEqualTo("25日");
    }

    /**
     * END_OF_MONTH の表示ラベルを確認する。
     */
    @Test
    void toLabel_shouldReturnEndOfMonthLabel_whenEndOfMonth() {
        String label = DayRuleUtils.toLabel(
                DayRuleType.END_OF_MONTH,
                null
        );

        assertThat(label).isEqualTo("末日");
    }

    /**
     * フロント返却用 DTO に type / value / label が入ることを確認する。
     */
    @Test
    void toResponse_shouldReturnDayRuleResponse() {
        DayRuleResponse response = DayRuleUtils.toResponse(
                DayRuleType.DAY_OF_MONTH,
                15
        );

        assertThat(response.type()).isEqualTo(DayRuleType.DAY_OF_MONTH);
        assertThat(response.value()).isEqualTo(15);
        assertThat(response.label()).isEqualTo("15日");
    }
}