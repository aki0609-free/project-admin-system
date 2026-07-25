package com.project.backend.features.system.rule.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.exception.RuleConflictException;
import com.project.backend.features.system.rule.mapper.RuleMasterMapper;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.features.system.rule.service.validation.RuleMasterValidator;
import com.project.backend.features.system.rule.service.validation.RuleReferenceChecker;

class RuleMasterCommandServiceTest {

    private RuleMasterRepository repository;
    private RuleReferenceChecker referenceChecker;
    private RuleMasterCommandService service;

    @BeforeEach
    void setUp() {
        repository = mock(RuleMasterRepository.class);
        referenceChecker = mock(RuleReferenceChecker.class);
        service = new RuleMasterCommandService(
                repository,
                mock(RuleMasterMapper.class),
                mock(RuleMasterValidator.class),
                referenceChecker
        );
    }

    @Test
    void delete_shouldRejectReferencedRule() {
        RuleMaster rule = new RuleMaster();
        rule.setId(1L);
        rule.setRuleName("OVERTIME_ALLOWANCE");

        when(repository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rule));
        when(referenceChecker.findReferenceTypes(
                "OVERTIME_ALLOWANCE"
        )).thenReturn(List.of("手当マスタ"));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(RuleConflictException.class)
                .hasMessageContaining("手当マスタ");

        verify(repository, never()).delete(rule);
    }
}
