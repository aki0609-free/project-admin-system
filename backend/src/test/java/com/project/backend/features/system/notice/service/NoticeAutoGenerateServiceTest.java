package com.project.backend.features.system.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.notice.dto.NoticeGenerateResult;
import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.GenerateResult;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;
import com.project.backend.features.system.notice.service.generator.NoticeGenerator;
import com.project.backend.features.system.notice.service.resolver.NoticeTargetResolverDispatcher;

import jakarta.persistence.EntityNotFoundException;

class NoticeAutoGenerateServiceTest {

    private static final LocalDate FIXED_BUSINESS_DATE =
            LocalDate.of(2026, 8, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-31T15:30:00Z"),
            ZoneId.of("Asia/Tokyo")
    );

    private NoticeRuleRepository ruleRepository;
    private NoticeTargetResolverDispatcher targetResolver;
    private NoticeGenerator noticeGenerator;
    private NoticeAutoGenerateService service;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(NoticeRuleRepository.class);
        targetResolver = mock(
                NoticeTargetResolverDispatcher.class
        );
        noticeGenerator = mock(NoticeGenerator.class);
        service = new NoticeAutoGenerateService(
                ruleRepository,
                targetResolver,
                noticeGenerator,
                FIXED_CLOCK
        );
    }

    @Test
    void generateByRuleId_shouldDelegateEachTargetAndCountResults() {
        NoticeRule rule = rule(1L, "CLOSING_NOTICE");
        NoticeTargetRow generatedTarget = target("1");
        NoticeTargetRow skippedTarget = target("2");

        when(ruleRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rule));
        when(targetResolver.resolve(rule))
                .thenReturn(List.of(
                        generatedTarget,
                        skippedTarget
                ));
        when(noticeGenerator.generate(
                org.mockito.ArgumentMatchers.eq(rule),
                org.mockito.ArgumentMatchers.eq(generatedTarget),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(GenerateResult.GENERATED);
        when(noticeGenerator.generate(
                org.mockito.ArgumentMatchers.eq(rule),
                org.mockito.ArgumentMatchers.eq(skippedTarget),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(GenerateResult.SKIPPED);

        NoticeGenerateResult result =
                service.generateByRuleId(1L);

        assertThat(result.ruleCount()).isEqualTo(1);
        assertThat(result.targetCount()).isEqualTo(2);
        assertThat(result.generatedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        verify(targetResolver).resolve(rule);
        verify(noticeGenerator).generate(
                rule,
                generatedTarget,
                FIXED_BUSINESS_DATE
        );
        verify(noticeGenerator).generate(
                rule,
                skippedTarget,
                FIXED_BUSINESS_DATE
        );
    }

    @Test
    void generateAll_shouldAggregateAllActiveRules() {
        NoticeRule first = rule(1L, "FIRST");
        NoticeRule second = rule(2L, "SECOND");
        NoticeTargetRow firstTarget = target("1");
        NoticeTargetRow secondTarget = target("2");

        when(ruleRepository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc())
                .thenReturn(List.of(first, second));
        when(targetResolver.resolve(first))
                .thenReturn(List.of(firstTarget));
        when(targetResolver.resolve(second))
                .thenReturn(List.of(secondTarget));
        when(noticeGenerator.generate(
                org.mockito.ArgumentMatchers.eq(first),
                org.mockito.ArgumentMatchers.eq(firstTarget),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(GenerateResult.GENERATED);
        when(noticeGenerator.generate(
                org.mockito.ArgumentMatchers.eq(second),
                org.mockito.ArgumentMatchers.eq(secondTarget),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(GenerateResult.SKIPPED);

        NoticeGenerateResult result = service.generateAll();

        assertThat(result.ruleCount()).isEqualTo(2);
        assertThat(result.targetCount()).isEqualTo(2);
        assertThat(result.generatedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
    }

    @Test
    void generateByRuleId_shouldReturnNotFound() {
        when(ruleRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.generateByRuleId(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private NoticeRule rule(
            Long id,
            String ruleCode
    ) {
        NoticeRule rule = new NoticeRule();
        rule.setId(id);
        rule.setRuleCode(ruleCode);
        return rule;
    }

    private NoticeTargetRow target(String key) {
        return NoticeTargetRow.builder()
                .targetKey(key)
                .targetDate(LocalDate.of(2026, 7, 31))
                .targetLabel("対象" + key)
                .build();
    }
}
