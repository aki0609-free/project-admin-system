package com.project.backend.features.system.calendar.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.dto.CalendarHolidayResponse;
import com.project.backend.features.system.calendar.dto.CalendarHolidaySaveRequest;
import com.project.backend.features.system.calendar.entity.SystemCalendarHoliday;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarHolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarHolidayManagementService {
    private final SystemCalendarHolidayRepository repository;
    private final CalendarDefaultHolidayProvisioner defaultHolidayProvisioner;

    public List<CalendarHolidayResponse> findByYear(CalendarType type, int year) {
        validateYear(year);
        if (type == CalendarType.COMPANY) defaultHolidayProvisioner.ensureCompanyDefaults(year);
        return repository
                .findByTenantIdAndCalendarTypeAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
                        TenantContext.getTenantId(), type,
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CalendarHolidayResponse save(CalendarHolidaySaveRequest request) {
        requireCompanyCalendar(request.calendarType());
        String tenantId = TenantContext.getTenantId();
        SystemCalendarHoliday entity;
        if (request.id() != null && request.id() > 0) {
            entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(request.id(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("休日設定が見つかりません。"));
            requireCompanyCalendar(entity.getCalendarType());
        } else {
            entity = repository
                    .findByTenantIdAndCalendarTypeAndHolidayDateAndDeletedAtIsNull(
                            tenantId, CalendarType.COMPANY, request.holidayDate()
                    )
                    .orElseGet(SystemCalendarHoliday::new);
        }
        entity.setTenantId(tenantId);
        entity.setCalendarType(CalendarType.COMPANY);
        entity.setHolidayDate(request.holidayDate());
        entity.setHolidayName(request.holidayName().trim());
        entity.setNote(normalize(request.note()));
        entity.setActiveFlag(Boolean.TRUE.equals(request.activeFlag()));
        entity.setDayType(request.dayType());
        return toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        SystemCalendarHoliday entity = repository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("休日設定が見つかりません。"));
        requireCompanyCalendar(entity.getCalendarType());
        entity.setActiveFlag(false);
    }

    private void validateYear(int year) {
        if (year < 2000 || year > 2200) {
            throw new IllegalArgumentException("対象年が不正です。");
        }
    }

    private void requireCompanyCalendar(CalendarType type) {
        if (type != CalendarType.COMPANY) {
            throw new IllegalArgumentException("編集できるのは会社休日だけです。");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CalendarHolidayResponse toResponse(SystemCalendarHoliday entity) {
        return new CalendarHolidayResponse(
                entity.getId(), entity.getCalendarType(), entity.getHolidayDate(),
                entity.getHolidayName(), entity.getNote(), entity.isActiveFlag(),
                entity.getCalendarType() == CalendarType.COMPANY,
                entity.getDayType()
        );
    }
}
