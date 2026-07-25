package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.common.closing.service.ClosingSettingQueryService;
import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;

class MonthlyClosingPeriodServiceTest {

    private ClosingSettingQueryService closingSettingQueryService;
    private MonthlyClosingPeriodService service;

    @BeforeEach
    void setUp() {
        closingSettingQueryService =
                mock(ClosingSettingQueryService.class);
        service = new MonthlyClosingPeriodService(
                closingSettingQueryService
        );
    }

    @Test
    void resolve_shouldUseCalendarMonth_whenEndOfMonthRule() {
        useRule(DayRuleType.END_OF_MONTH, null);

        MonthlyClosingPeriod result =
                service.resolve("2026-04");

        assertThat(result.targetMonth()).isEqualTo("2026-04");
        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void resolve_shouldStartAfterPreviousClosingDay() {
        useRule(DayRuleType.DAY_OF_MONTH, 20);

        MonthlyClosingPeriod result =
                service.resolve("2026-05");

        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    void resolve_shouldClamp31DayRuleAcrossNonLeapFebruary() {
        useRule(DayRuleType.DAY_OF_MONTH, 31);

        MonthlyClosingPeriod result =
                service.resolve("2026-02");

        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void resolve_shouldClamp31DayRuleAcrossLeapFebruary() {
        useRule(DayRuleType.DAY_OF_MONTH, 31);

        MonthlyClosingPeriod result =
                service.resolve("2024-02");

        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void resolve_shouldCrossYearBoundary() {
        useRule(DayRuleType.DAY_OF_MONTH, 20);

        MonthlyClosingPeriod result =
                service.resolve("2027-01");

        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2026, 12, 21));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2027, 1, 20));
    }

    @Test
    void resolve_shouldUseLastDay_whenDayValueIsMissing() {
        useRule(DayRuleType.DAY_OF_MONTH, null);

        MonthlyClosingPeriod result =
                service.resolve("2026-03");

        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2026, 3, 31));
    }

    private void useRule(
            DayRuleType type,
            Integer value
    ) {
        when(closingSettingQueryService
                .getPayrollClosingDayRule())
                .thenReturn(DayRule.builder()
                        .type(type)
                        .value(value)
                        .monthOffset(0)
                        .build());
    }
}
