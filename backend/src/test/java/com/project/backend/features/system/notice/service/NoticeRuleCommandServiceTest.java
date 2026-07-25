package com.project.backend.features.system.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.mapper.NoticeRuleMapper;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;
import com.project.backend.features.system.notice.service.validation.NoticeRuleValidator;

class NoticeRuleCommandServiceTest {

    @Test
    void delete_shouldUseApplicationClockAndCancelSchedule() {
        Instant fixedInstant =
                Instant.parse("2026-07-31T15:30:00Z");
        Clock fixedClock = Clock.fixed(
                fixedInstant,
                ZoneId.of("Asia/Tokyo")
        );

        NoticeRuleRepository repository =
                mock(NoticeRuleRepository.class);
        NoticeDynamicSchedulerService scheduler =
                mock(NoticeDynamicSchedulerService.class);
        NoticeRule rule = new NoticeRule();
        rule.setId(1L);

        when(repository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rule));

        NoticeRuleCommandService service =
                new NoticeRuleCommandService(
                        repository,
                        mock(NoticeRuleMapper.class),
                        mock(NoticeRuleValidator.class),
                        scheduler,
                        fixedClock
                );

        service.delete(1L);

        assertThat(rule.getDeletedAt())
                .isEqualTo(fixedInstant);
        verify(scheduler).cancel(1L);
    }
}
