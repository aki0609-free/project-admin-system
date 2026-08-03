package com.project.backend.features.dailyreport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.dailyreport.entity.DailyPayRuleSetting;

public interface DailyPayRuleSettingRepository
        extends JpaRepository<DailyPayRuleSetting, Long> {

    List<DailyPayRuleSetting>
            findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                    String tenantId
            );
}
