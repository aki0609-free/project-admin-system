package com.project.backend.features.system.calendar.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.enums.CalendarDayType;
import com.project.backend.features.system.calendar.repository.SystemCalendarHolidayRepository;
import com.project.backend.features.system.calendar.repository.SystemCalendarReferenceRepository;
import com.project.backend.features.system.calendar.repository.SystemCalendarWeekdayRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarQueryService {
    private final SystemCalendarHolidayRepository repository;
    private final SystemCalendarWeekdayRuleRepository weekdayRuleRepository;
    private final SystemCalendarReferenceRepository referenceRepository;
    private final CalendarDefaultHolidayProvisioner defaultHolidayProvisioner;

    public boolean isClosed(LocalDate date, CalendarDayPolicy policy) {
        if (date == null) return false;
        if (policy.closedWeekdays().contains(date.getDayOfWeek())) return true;
        Set<CalendarType> types = policy.calendarTypes();
        if (types.isEmpty()) return false;
        return types.stream().anyMatch(type -> isCalendarClosed(date, type, new HashSet<>()));
    }

    private boolean isCalendarClosed(
            LocalDate date,
            CalendarType type,
            Set<CalendarType> resolving
    ) {
        if (!resolving.add(type)) {
            throw new IllegalStateException("カレンダーの参照設定が循環しています。");
        }
        try {
            return switch (type) {
                case NATIONAL_HOLIDAY -> hasStoredClosedDay(CalendarType.NATIONAL_HOLIDAY, date);
                case COMPANY -> isCompanyClosed(date, resolving);
                case GOVERNMENT -> isGovernmentClosed(date);
                case FINANCIAL_INSTITUTION -> isFinancialInstitutionClosed(date);
            };
        } finally {
            resolving.remove(type);
        }
    }

    private boolean isCompanyClosed(LocalDate date, Set<CalendarType> resolving) {
        defaultHolidayProvisioner.ensureCompanyDefaults(date.getYear());
        String tenantId = TenantContext.getTenantId();
        var override = repository
                .findByTenantIdAndCalendarTypeAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        tenantId, CalendarType.COMPANY, date
                );
        if (override.isPresent()) {
            return override.get().getDayType() == CalendarDayType.CLOSED;
        }
        if (weekdayRuleRepository
                .existsByTenantIdAndCalendarTypeAndDayOfWeekAndClosedFlagTrueAndDeletedAtIsNull(
                        tenantId, CalendarType.COMPANY, date.getDayOfWeek()
                )) {
            return true;
        }
        return referenceRepository
                .findByTenantIdAndCalendarTypeAndActiveFlagTrueAndDeletedAtIsNull(
                        tenantId, CalendarType.COMPANY
                )
                .stream()
                .anyMatch(reference -> isCalendarClosed(
                        date, reference.getSourceCalendarType(), resolving
                ));
    }

    private boolean isGovernmentClosed(LocalDate date) {
        return CalendarFixedRuleCatalog.isClosedByFixedRule(CalendarType.GOVERNMENT, date)
                || hasStoredClosedDay(CalendarType.GOVERNMENT, date)
                || hasStoredClosedDay(CalendarType.NATIONAL_HOLIDAY, date);
    }

    private boolean isFinancialInstitutionClosed(LocalDate date) {
        return CalendarFixedRuleCatalog.isClosedByFixedRule(
                        CalendarType.FINANCIAL_INSTITUTION, date
                )
                || hasStoredClosedDay(CalendarType.FINANCIAL_INSTITUTION, date)
                || hasStoredClosedDay(CalendarType.NATIONAL_HOLIDAY, date);
    }

    private boolean hasStoredClosedDay(CalendarType type, LocalDate date) {
        return repository
                .findByTenantIdAndCalendarTypeAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId(), type, date
                )
                .filter(item -> item.getDayType() == CalendarDayType.CLOSED)
                .isPresent();
    }

}
