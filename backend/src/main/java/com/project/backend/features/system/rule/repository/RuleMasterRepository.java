package com.project.backend.features.system.rule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.enums.RuleType;

public interface RuleMasterRepository extends JpaRepository<RuleMaster, Long> {

    List<RuleMaster> findAllByDeletedAtIsNullOrderByIdAsc();

    List<RuleMaster> findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

    List<RuleMaster> findByTenantIdAndActiveFlagTrueAndDeletedAtIsNullOrderByRuleNameAsc(
            String tenantId
    );
    List<RuleMaster> findByTenantIdAndRuleTypeInAndActiveFlagTrueAndDeletedAtIsNullOrderByRuleNameAsc(
            String tenantId,
            List<RuleType> ruleTypes
    );

    Optional<RuleMaster> findByIdAndDeletedAtIsNull(Long id);

    Optional<RuleMaster> findByRuleNameAndDeletedAtIsNull(String ruleName);

    Optional<RuleMaster> findByRuleNameAndActiveFlagTrueAndDeletedAtIsNull(String ruleName);

    boolean existsByTenantIdAndRuleNameAndActiveFlagTrueAndDeletedAtIsNull(
            String tenantId,
            String ruleName
    );
    Optional<RuleMaster> findByTenantIdAndRuleNameAndActiveFlagTrueAndDeletedAtIsNull(
            String tenantId,
            String ruleName
    );

    boolean existsByRuleNameAndDeletedAtIsNull(String ruleName);

    boolean existsByRuleNameAndIdNotAndDeletedAtIsNull(String ruleName, Long id);
}
