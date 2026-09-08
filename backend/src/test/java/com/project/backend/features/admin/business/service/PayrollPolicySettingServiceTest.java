package com.project.backend.features.admin.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.PayrollPolicySettingSaveRequest;
import com.project.backend.features.admin.business.entity.PayrollPolicySetting;
import com.project.backend.features.admin.business.repository.PayrollPolicySettingRepository;

class PayrollPolicySettingServiceTest {

    private final PayrollPolicySettingRepository repository =
            mock(PayrollPolicySettingRepository.class);
    private final PayrollPolicySettingService service =
            new PayrollPolicySettingService(
                    repository,
                    Clock.fixed(
                            Instant.parse("2026-09-05T00:00:00Z"),
                            ZoneOffset.UTC
                    )
            );

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolve_shouldReturnDefaultsWhenNoSettingExists() {
        LocalDate targetDate = LocalDate.of(2026, 9, 5);
        when(repository.findEffectiveSettings("tenant-a", targetDate))
                .thenReturn(List.of());

        var result = service.resolve(targetDate);

        assertThat(result.weekStartDay()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.weeklyStatutoryHours())
                .isEqualByComparingTo("40.00");
        assertThat(result.amountRoundingMode())
                .isEqualTo(RoundingMode.HALF_UP);
    }

    @Test
    void resolve_shouldUseSettingEffectiveOnTargetDate() {
        LocalDate targetDate = LocalDate.of(2026, 10, 1);
        PayrollPolicySetting setting = new PayrollPolicySetting();
        setting.setWeekStartDay(DayOfWeek.SUNDAY);
        setting.setWeeklyStatutoryHours(new BigDecimal("38.00"));
        setting.setMonthlyOvertimeThresholdHours(new BigDecimal("55.00"));
        setting.setOvertimeRate(new BigDecimal("1.300"));
        setting.setOvertimeOverThresholdRate(new BigDecimal("1.550"));
        setting.setNightPremiumRate(new BigDecimal("0.300"));
        setting.setStatutoryHolidayRate(new BigDecimal("1.400"));
        setting.setDailyStandardHours(new BigDecimal("7.50"));
        setting.setAmountRoundingMode(RoundingMode.DOWN);
        when(repository.findEffectiveSettings("tenant-a", targetDate))
                .thenReturn(List.of(setting));

        var result = service.resolve(targetDate);

        assertThat(result.weekStartDay()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(result.overtimeRate()).isEqualByComparingTo("1.300");
        assertThat(result.dailyStandardHours()).isEqualByComparingTo("7.50");
        assertThat(result.amountRoundingMode()).isEqualTo(RoundingMode.DOWN);
    }

    @Test
    void save_shouldRejectOverlappingActivePeriod() {
        PayrollPolicySetting existing = new PayrollPolicySetting();
        existing.setId(10L);
        existing.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        existing.setEffectiveTo(null);
        existing.setActiveFlag(true);
        when(repository
                .findByTenantIdAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(
                        "tenant-a"
                ))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(request(
                LocalDate.of(2026, 10, 1), null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("適用期間が既存設定と重複");
    }

    private PayrollPolicySettingSaveRequest request(
            LocalDate from,
            LocalDate to
    ) {
        return new PayrollPolicySettingSaveRequest(
                null,
                from,
                to,
                DayOfWeek.MONDAY,
                new BigDecimal("40.00"),
                new BigDecimal("60.00"),
                new BigDecimal("1.250"),
                new BigDecimal("1.500"),
                new BigDecimal("0.250"),
                new BigDecimal("1.350"),
                new BigDecimal("8.00"),
                RoundingMode.HALF_UP,
                true
        );
    }
}
