package com.project.backend.features.admin.business.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.BusinessHolidayResponse;
import com.project.backend.features.admin.business.dto.BusinessHolidaySaveRequest;
import com.project.backend.features.admin.business.entity.BusinessHoliday;
import com.project.backend.features.admin.business.repository.BusinessHolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessHolidayService {
    private final BusinessHolidayRepository repository;
    private final Clock clock;

    public List<BusinessHolidayResponse> findByYear(int year) {
        validateYear(year);
        ensureObonRows(TenantContext.getTenantId(), year);
        return repository.findByTenantIdAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
                TenantContext.getTenantId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .stream().map(this::toResponse).toList();
    }

    public BusinessHolidayResponse save(BusinessHolidaySaveRequest request) {
        if (!"OBON".equals(request.holidayType())) {
            throw new IllegalArgumentException("休日区分が不正です。");
        }
        validateObonDate(request.holidayDate());
        String tenantId = TenantContext.getTenantId();
        BusinessHoliday entity;
        if (request.id() != null && request.id() > 0) {
            entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(request.id(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("休日設定が見つかりません。"));
        } else {
            entity = repository.findByTenantIdAndHolidayDateAndDeletedAtIsNull(
                    tenantId, request.holidayDate()).orElseGet(BusinessHoliday::new);
        }
        entity.setHolidayDate(request.holidayDate());
        entity.setHolidayName(request.holidayName().trim());
        entity.setHolidayType(request.holidayType());
        entity.setNote(normalize(request.note()));
        entity.setActiveFlag(Boolean.TRUE.equals(request.activeFlag()));
        return toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        BusinessHoliday entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(
                id, TenantContext.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("休日設定が見つかりません。"));
        entity.setDeletedAt(Instant.now(clock));
    }

    private void validateYear(int year) {
        if (year < 2000 || year > 2200) throw new IllegalArgumentException("対象年が不正です。");
    }

    private void validateObonDate(LocalDate date) {
        if (date.getMonthValue() != 8 || date.getDayOfMonth() < 13 || date.getDayOfMonth() > 15) {
            throw new IllegalArgumentException("休日は8月13日〜15日のお盆休みだけ登録できます。");
        }
    }

    private void ensureObonRows(String tenantId, int year) {
        for (int day = 13; day <= 15; day++) {
            LocalDate date = LocalDate.of(year, 8, day);
            if (repository.findByTenantIdAndHolidayDateAndDeletedAtIsNull(tenantId, date).isPresent()) {
                continue;
            }
            BusinessHoliday holiday = new BusinessHoliday();
            holiday.setHolidayDate(date);
            holiday.setHolidayName("お盆休み");
            holiday.setHolidayType("OBON");
            holiday.setActiveFlag(true);
            repository.save(holiday);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessHolidayResponse toResponse(BusinessHoliday entity) {
        return new BusinessHolidayResponse(entity.getId(), entity.getHolidayDate(),
                entity.getHolidayName(), entity.getHolidayType(), entity.getNote(), entity.isActiveFlag());
    }
}
