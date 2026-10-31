package com.project.backend.features.admin.business.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.admin.business.entity.BusinessHoliday;

public interface BusinessHolidayRepository extends JpaRepository<BusinessHoliday, Long> {
    List<BusinessHoliday> findByTenantIdAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
            String tenantId, LocalDate from, LocalDate to);
    Optional<BusinessHoliday> findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);
    Optional<BusinessHoliday> findByTenantIdAndHolidayDateAndDeletedAtIsNull(
            String tenantId, LocalDate holidayDate);
    boolean existsByTenantIdAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
            String tenantId, LocalDate holidayDate);
}
