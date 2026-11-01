package com.project.backend.features.system.calendar.service;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.dto.CalendarPolicyResponse;
import com.project.backend.features.system.calendar.dto.CalendarPolicySaveRequest;
import com.project.backend.features.system.calendar.dto.CalendarFixedPeriodResponse;
import com.project.backend.features.system.calendar.entity.SystemCalendarReference;
import com.project.backend.features.system.calendar.entity.SystemCalendarWeekdayRule;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarReferenceRepository;
import com.project.backend.features.system.calendar.repository.SystemCalendarWeekdayRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarPolicyService {
    private static final Set<CalendarType> ALLOWED_COMPANY_SOURCES = Set.of(
            CalendarType.GOVERNMENT, CalendarType.FINANCIAL_INSTITUTION
    );

    private final SystemCalendarWeekdayRuleRepository weekdayRuleRepository;
    private final SystemCalendarReferenceRepository referenceRepository;
    private final CalendarDefaultHolidayProvisioner defaultHolidayProvisioner;

    public CalendarPolicyResponse find(CalendarType type) {
        if (type == CalendarType.COMPANY) {
            defaultHolidayProvisioner.ensureCompanyPolicyDefaults();
            return companyPolicy(true);
        }
        return fixedPolicy(type);
    }

    public CalendarPolicyResponse save(CalendarPolicySaveRequest request) {
        requireCompany(request.calendarType());
        if (!ALLOWED_COMPANY_SOURCES.containsAll(request.includedCalendarTypes())) {
            throw new IllegalArgumentException("会社休日で参照できないカレンダーが含まれています。");
        }
        String tenantId = TenantContext.getTenantId();
        syncWeekdays(tenantId, request.closedWeekdays());
        syncReferences(tenantId, request.includedCalendarTypes());
        return companyPolicy(true);
    }

    private CalendarPolicyResponse companyPolicy(boolean editable) {
        String tenantId = TenantContext.getTenantId();
        Set<DayOfWeek> weekdays = weekdayRuleRepository
                .findByTenantIdAndCalendarTypeAndDeletedAtIsNull(
                        tenantId, CalendarType.COMPANY
                )
                .stream()
                .filter(SystemCalendarWeekdayRule::isClosedFlag)
                .map(SystemCalendarWeekdayRule::getDayOfWeek)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
        Set<CalendarType> sources = referenceRepository
                .findByTenantIdAndCalendarTypeAndActiveFlagTrueAndDeletedAtIsNull(
                        tenantId, CalendarType.COMPANY
                )
                .stream()
                .map(SystemCalendarReference::getSourceCalendarType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(CalendarType.class)));
        return new CalendarPolicyResponse(
                CalendarType.COMPANY, weekdays, sources, List.of(), editable
        );
    }

    private CalendarPolicyResponse fixedPolicy(CalendarType type) {
        if (type == CalendarType.COMPANY) {
            throw new IllegalStateException("会社休日は可変設定です。");
        }
        Set<CalendarType> sources = switch (type) {
            case GOVERNMENT, FINANCIAL_INSTITUTION ->
                    Set.of(CalendarType.NATIONAL_HOLIDAY);
            case NATIONAL_HOLIDAY -> Set.of();
            case COMPANY -> throw new IllegalStateException("会社休日は可変設定です。");
        };
        List<CalendarFixedPeriodResponse> periods = CalendarFixedRuleCatalog
                .fixedPeriods(type)
                .stream()
                .map(period -> new CalendarFixedPeriodResponse(
                        period.name(),
                        period.start().toString().substring(2),
                        period.end().toString().substring(2)
                ))
                .toList();
        return new CalendarPolicyResponse(
                type,
                CalendarFixedRuleCatalog.closedWeekdays(type),
                sources,
                periods,
                false
        );
    }

    private void syncWeekdays(String tenantId, Set<DayOfWeek> selected) {
        List<SystemCalendarWeekdayRule> existing = weekdayRuleRepository
                .findByTenantIdAndCalendarTypeAndDeletedAtIsNull(
                        tenantId, CalendarType.COMPANY
                );
        for (DayOfWeek day : DayOfWeek.values()) {
            SystemCalendarWeekdayRule rule = existing.stream()
                    .filter(item -> item.getDayOfWeek() == day)
                    .findFirst()
                    .orElseGet(() -> newWeekdayRule(tenantId, day));
            rule.setClosedFlag(selected.contains(day));
            weekdayRuleRepository.save(rule);
        }
    }

    private void syncReferences(String tenantId, Set<CalendarType> selected) {
        List<SystemCalendarReference> existing = referenceRepository
                .findByTenantIdAndCalendarTypeAndDeletedAtIsNull(
                        tenantId, CalendarType.COMPANY
                );
        for (CalendarType source : ALLOWED_COMPANY_SOURCES) {
            SystemCalendarReference reference = existing.stream()
                    .filter(item -> item.getSourceCalendarType() == source)
                    .findFirst()
                    .orElseGet(() -> newReference(tenantId, source));
            reference.setActiveFlag(selected.contains(source));
            referenceRepository.save(reference);
        }
    }

    private SystemCalendarWeekdayRule newWeekdayRule(String tenantId, DayOfWeek day) {
        SystemCalendarWeekdayRule rule = new SystemCalendarWeekdayRule();
        rule.setTenantId(tenantId);
        rule.setCalendarType(CalendarType.COMPANY);
        rule.setDayOfWeek(day);
        return rule;
    }

    private SystemCalendarReference newReference(String tenantId, CalendarType source) {
        SystemCalendarReference reference = new SystemCalendarReference();
        reference.setTenantId(tenantId);
        reference.setCalendarType(CalendarType.COMPANY);
        reference.setSourceCalendarType(source);
        return reference;
    }

    private void requireCompany(CalendarType type) {
        if (type != CalendarType.COMPANY) {
            throw new IllegalArgumentException("編集できるのは会社休日だけです。");
        }
    }
}
