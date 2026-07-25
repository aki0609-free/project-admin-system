package com.project.backend.features.system.rule.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.rule.dto.RuleMasterSaveRequest;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.enums.RuleType;
import com.project.backend.features.system.rule.exception.RuleConflictException;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.features.system.rule.service.RuleDataSourceCatalogService;
import com.project.backend.features.system.rule.service.RuleBeanCatalogService;

class RuleMasterValidatorTest {

    private RuleMasterRepository repository;
    private RuleMasterValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(RuleMasterRepository.class);
        validator = new RuleMasterValidator(
                repository,
                mock(RuleDataSourceCatalogService.class),
                mock(RuleBeanCatalogService.class)
        );
    }

    @Test
    void validateForUpdate_shouldRejectRuleNameChange() {
        RuleMaster current = new RuleMaster();
        current.setId(10L);
        current.setRuleName("OVERTIME_ALLOWANCE");

        when(repository
                .existsByRuleNameAndIdNotAndDeletedAtIsNull(
                        "RENAMED_RULE",
                        10L
                ))
                .thenReturn(false);

        assertThatThrownBy(() ->
                validator.validateForUpdate(
                        current,
                        validRequest(
                                "RENAMED_RULE",
                                "params.hours * 1000"
                        )
                ))
                .isInstanceOf(RuleConflictException.class)
                .hasMessageContaining("変更できません");
    }

    @Test
    void validateForCreate_shouldRejectUnsafeSqlClause() {
        var source = new com.project.backend.features.system.rule.dto.RuleDataSourceSaveRequest(
                null,
                "employee",
                null,
                "employees",
                "tenant_id = :tenantId; DROP TABLE users",
                true,
                true,
                1,
                List.of()
        );
        RuleMasterSaveRequest request = new RuleMasterSaveRequest(
                "SAFE_RULE",
                "安全なRule",
                RuleType.ALLOWANCE,
                RuleDslType.JEXL,
                "params.amount",
                null,
                "result",
                null,
                100,
                true,
                List.of(),
                List.of(source)
        );

        assertThatThrownBy(() ->
                validator.validateForCreate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("whereClause");
    }

    @Test
    void validateForCreate_shouldRejectUnsafeDsl() {
        assertThatThrownBy(() ->
                validator.validateForCreate(validRequest(
                        "UNSAFE_RULE",
                        "Runtime.getRuntime().exec('command')"
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DSL");
    }

    private RuleMasterSaveRequest validRequest(
            String ruleName,
            String dslText
    ) {
        return new RuleMasterSaveRequest(
                ruleName,
                "Rule表示名",
                RuleType.ALLOWANCE,
                RuleDslType.JEXL,
                dslText,
                null,
                "result",
                null,
                100,
                true,
                List.of(),
                List.of()
        );
    }
}
