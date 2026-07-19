package com.project.backend.features.system.notice.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.notice.entity.NoticeGenerated;

public interface NoticeGeneratedRepository extends JpaRepository<NoticeGenerated, Long> {

    boolean existsByRuleCodeAndTargetTableNameAndTargetKeyAndTargetDateAndDeletedAtIsNull(
            String ruleCode,
            String targetTableName,
            String targetKey,
            LocalDate targetDate
    );
}