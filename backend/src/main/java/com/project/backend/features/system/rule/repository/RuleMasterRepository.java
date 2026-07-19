package com.project.backend.features.system.rule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.rule.entity.RuleMaster;

public interface RuleMasterRepository extends JpaRepository<RuleMaster, Long> {

    List<RuleMaster> findAllByDeletedAtIsNullOrderByIdAsc();

    List<RuleMaster> findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

    Optional<RuleMaster> findByIdAndDeletedAtIsNull(Long id);

    Optional<RuleMaster> findByRuleNameAndDeletedAtIsNull(String ruleName);

    Optional<RuleMaster> findByRuleNameAndActiveFlagTrueAndDeletedAtIsNull(String ruleName);

    boolean existsByRuleNameAndDeletedAtIsNull(String ruleName);

    boolean existsByRuleNameAndIdNotAndDeletedAtIsNull(String ruleName, Long id);
}