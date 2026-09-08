package com.project.backend.features.admin.business.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.PayrollPolicySettingResponse;
import com.project.backend.features.admin.business.dto.PayrollPolicySettingSaveRequest;
import com.project.backend.features.admin.business.entity.PayrollPolicySetting;
import com.project.backend.features.admin.business.repository.PayrollPolicySettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollPolicySettingService {

    private final PayrollPolicySettingRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PayrollPolicySettingResponse> findAll() {
        return repository
                .findByTenantIdAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(
                        TenantContext.getTenantId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PayrollPolicySettingResponse save(
            PayrollPolicySettingSaveRequest request
    ) {
        validatePeriod(request.effectiveFrom(), request.effectiveTo());
        String tenantId = TenantContext.getTenantId();
        PayrollPolicySetting entity = request.id() == null || request.id() <= 0
                ? new PayrollPolicySetting()
                : repository.findByIdAndTenantIdAndDeletedAtIsNull(
                        request.id(), tenantId
                ).orElseThrow(() -> new IllegalArgumentException(
                        "給与制度設定が見つかりません。id=" + request.id()
                ));

        if (Boolean.TRUE.equals(request.activeFlag())) {
            validateNoOverlap(entity.getId(), request.effectiveFrom(), request.effectiveTo());
        }
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        PayrollPolicySetting entity = repository
                .findByIdAndTenantIdAndDeletedAtIsNull(
                        id, TenantContext.getTenantId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "給与制度設定が見つかりません。id=" + id
                ));
        entity.setDeletedAt(Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public PayrollPolicyValues resolve(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("給与制度の判定対象日は必須です。");
        }
        List<PayrollPolicySetting> matches = repository.findEffectiveSettings(
                TenantContext.getTenantId(), targetDate
        );
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "対象日に有効な給与制度設定が重複しています。targetDate=" + targetDate
            );
        }
        return matches.isEmpty()
                ? PayrollPolicyValues.defaults()
                : PayrollPolicyValues.from(matches.getFirst());
    }

    private void validateNoOverlap(
            Long editingId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        boolean overlaps = repository
                .findByTenantIdAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(
                        TenantContext.getTenantId()
                )
                .stream()
                .filter(PayrollPolicySetting::isActiveFlag)
                .filter(item -> editingId == null || !editingId.equals(item.getId()))
                .anyMatch(item -> overlaps(
                        effectiveFrom,
                        effectiveTo,
                        item.getEffectiveFrom(),
                        item.getEffectiveTo()
                ));
        if (overlaps) {
            throw new IllegalArgumentException(
                    "有効な給与制度設定の適用期間が既存設定と重複しています。"
            );
        }
    }

    private boolean overlaps(
            LocalDate leftFrom,
            LocalDate leftTo,
            LocalDate rightFrom,
            LocalDate rightTo
    ) {
        return (leftTo == null || !leftTo.isBefore(rightFrom))
                && (rightTo == null || !rightTo.isBefore(leftFrom));
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (to != null && to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "適用終了日は適用開始日以降を指定してください。"
            );
        }
    }

    private void apply(
            PayrollPolicySetting entity,
            PayrollPolicySettingSaveRequest request
    ) {
        entity.setEffectiveFrom(request.effectiveFrom());
        entity.setEffectiveTo(request.effectiveTo());
        entity.setWeekStartDay(request.weekStartDay());
        entity.setWeeklyStatutoryHours(request.weeklyStatutoryHours());
        entity.setMonthlyOvertimeThresholdHours(
                request.monthlyOvertimeThresholdHours()
        );
        entity.setOvertimeRate(request.overtimeRate());
        entity.setOvertimeOverThresholdRate(
                request.overtimeOverThresholdRate()
        );
        entity.setNightPremiumRate(request.nightPremiumRate());
        entity.setStatutoryHolidayRate(request.statutoryHolidayRate());
        entity.setDailyStandardHours(request.dailyStandardHours());
        entity.setAmountRoundingMode(request.amountRoundingMode());
        entity.setActiveFlag(Boolean.TRUE.equals(request.activeFlag()));
    }

    private PayrollPolicySettingResponse toResponse(PayrollPolicySetting entity) {
        return new PayrollPolicySettingResponse(
                entity.getId(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.getWeekStartDay(),
                entity.getWeeklyStatutoryHours(),
                entity.getMonthlyOvertimeThresholdHours(),
                entity.getOvertimeRate(),
                entity.getOvertimeOverThresholdRate(),
                entity.getNightPremiumRate(),
                entity.getStatutoryHolidayRate(),
                entity.getDailyStandardHours(),
                entity.getAmountRoundingMode(),
                entity.isActiveFlag()
        );
    }

    public record PayrollPolicyValues(
            DayOfWeek weekStartDay,
            BigDecimal weeklyStatutoryHours,
            BigDecimal monthlyOvertimeThresholdHours,
            BigDecimal overtimeRate,
            BigDecimal overtimeOverThresholdRate,
            BigDecimal nightPremiumRate,
            BigDecimal statutoryHolidayRate,
            BigDecimal dailyStandardHours,
            RoundingMode amountRoundingMode
    ) {
        public static PayrollPolicyValues defaults() {
            return new PayrollPolicyValues(
                    DayOfWeek.MONDAY,
                    new BigDecimal("40.00"),
                    new BigDecimal("60.00"),
                    new BigDecimal("1.250"),
                    new BigDecimal("1.500"),
                    new BigDecimal("0.250"),
                    new BigDecimal("1.350"),
                    new BigDecimal("8.00"),
                    RoundingMode.HALF_UP
            );
        }

        public static PayrollPolicyValues from(PayrollPolicySetting setting) {
            return new PayrollPolicyValues(
                    setting.getWeekStartDay(),
                    setting.getWeeklyStatutoryHours(),
                    setting.getMonthlyOvertimeThresholdHours(),
                    setting.getOvertimeRate(),
                    setting.getOvertimeOverThresholdRate(),
                    setting.getNightPremiumRate(),
                    setting.getStatutoryHolidayRate(),
                    setting.getDailyStandardHours(),
                    setting.getAmountRoundingMode()
            );
        }
    }
}
