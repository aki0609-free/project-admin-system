package com.project.backend.features.admin.business.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.admin.business.entity.PayrollPolicySetting;

public interface PayrollPolicySettingRepository
        extends JpaRepository<PayrollPolicySetting, Long> {

    List<PayrollPolicySetting>
            findByTenantIdAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(
                    String tenantId
            );

    Optional<PayrollPolicySetting>
            findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);

    @Query("""
            select setting from PayrollPolicySetting setting
            where setting.tenantId = :tenantId
              and setting.activeFlag = true
              and setting.deletedAt is null
              and setting.effectiveFrom <= :targetDate
              and (setting.effectiveTo is null or setting.effectiveTo >= :targetDate)
            order by setting.effectiveFrom desc, setting.id desc
            """)
    List<PayrollPolicySetting> findEffectiveSettings(
            @Param("tenantId") String tenantId,
            @Param("targetDate") LocalDate targetDate
    );
}
