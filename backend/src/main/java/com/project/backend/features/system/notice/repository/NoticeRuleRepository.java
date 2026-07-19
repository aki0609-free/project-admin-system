package com.project.backend.features.system.notice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.notice.entity.NoticeRule;

public interface NoticeRuleRepository extends JpaRepository<NoticeRule, Long> {

    List<NoticeRule> findAllByDeletedAtIsNullOrderByIdAsc();

    List<NoticeRule> findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

    Optional<NoticeRule> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByRuleCodeAndDeletedAtIsNull(String ruleCode);

    boolean existsByRuleCodeAndIdNotAndDeletedAtIsNull(String ruleCode, Long id);
}