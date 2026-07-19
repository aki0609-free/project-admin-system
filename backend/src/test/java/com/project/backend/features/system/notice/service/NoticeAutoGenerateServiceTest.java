package com.project.backend.features.system.notice.service;

import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeSourceType;
import com.project.backend.features.dashboard.enums.NoticeType;
import com.project.backend.features.dashboard.repository.NoticeRepository;
import com.project.backend.features.dashboard.service.renderer.NoticeContentRenderer;
import com.project.backend.features.system.notice.dto.NoticeGenerateResult;
import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeGenerated;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.enums.NoticeSeverity;
import com.project.backend.features.system.notice.repository.NoticeGeneratedRepository;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;
import com.project.backend.features.system.notice.service.resolver.NoticeTargetResolverDispatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeAutoGenerateServiceTest {

    @Mock
    NoticeRuleRepository ruleRepository;

    @Mock
    NoticeGeneratedRepository generatedRepository;

    @Mock
    NoticeRepository noticeRepository;

    @Mock
    NoticeTargetResolverDispatcher noticeTargetResolver;

    @Mock
    NoticeContentRenderer noticeContentRenderer;

    @InjectMocks
    NoticeAutoGenerateService service;

    @SuppressWarnings("null")
    @Test
    void generateByRuleId_shouldGenerateNotice_whenTargetMatchesRule() {
        NoticeRule rule = validBeforeDaysRule();

        NoticeTargetRow targetRow = NoticeTargetRow.builder()
                .targetKey("1")
                .targetDate(LocalDate.now().plusDays(3))
                .targetLabel("株式会社A")
                .build();

        Notice savedNotice = new Notice();
        savedNotice.setId(100L);

        when(ruleRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rule));

        when(noticeTargetResolver.resolve(rule))
                .thenReturn(List.of(targetRow));

        when(generatedRepository.existsByRuleCodeAndTargetTableNameAndTargetKeyAndTargetDateAndDeletedAtIsNull(
                rule.getRuleCode(),
                rule.getTargetTableName(),
                "1",
                targetRow.targetDate()
        )).thenReturn(false);

        when(noticeContentRenderer.render(
                NoticeContentFormat.PLAIN_TEXT,
                "株式会社A の締め日は " + targetRow.targetDate() + " です。"
        )).thenReturn("株式会社A の締め日は " + targetRow.targetDate() + " です。");

        when(noticeRepository.save(any(Notice.class)))
                .thenReturn(savedNotice);

        NoticeGenerateResult result = service.generateByRuleId(1L);

        assertThat(result.ruleCount()).isEqualTo(1);
        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.generatedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(0);

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(noticeCaptor.capture());

        Notice notice = noticeCaptor.getValue();

        assertThat(notice.getTitle()).isEqualTo("株式会社A の締め日が近づいています");
        assertThat(notice.getContent()).isEqualTo("株式会社A の締め日は " + targetRow.targetDate() + " です。");
        assertThat(notice.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(notice.getEndDate()).isEqualTo(targetRow.targetDate());
        assertThat(notice.getType()).isEqualTo(NoticeType.INFO);
        assertThat(notice.getColor()).isEqualTo("blue");
        assertThat(notice.getContentFormat()).isEqualTo(NoticeContentFormat.PLAIN_TEXT);
        assertThat(notice.getSourceType()).isEqualTo(NoticeSourceType.AUTO);
        assertThat(notice.getSourceRuleCode()).isEqualTo(rule.getRuleCode());
        assertThat(notice.isPinnedFlag()).isFalse();
        assertThat(notice.isActiveFlag()).isTrue();

        verify(noticeContentRenderer).render(
                NoticeContentFormat.PLAIN_TEXT,
                "株式会社A の締め日は " + targetRow.targetDate() + " です。"
        );

        ArgumentCaptor<NoticeGenerated> generatedCaptor =
                ArgumentCaptor.forClass(NoticeGenerated.class);

        verify(generatedRepository).save(generatedCaptor.capture());

        NoticeGenerated generated = generatedCaptor.getValue();

        assertThat(generated.getRuleCode()).isEqualTo(rule.getRuleCode());
        assertThat(generated.getTargetTableName()).isEqualTo(rule.getTargetTableName());
        assertThat(generated.getTargetKey()).isEqualTo("1");
        assertThat(generated.getTargetDate()).isEqualTo(targetRow.targetDate());
        assertThat(generated.getGeneratedNoticeId()).isEqualTo(100L);
    }

    @SuppressWarnings("null")
    @Test
    void generateByRuleId_shouldGenerateHtmlNotice_whenRuleContentFormatIsHtml() {
        NoticeRule rule = validBeforeDaysRule();
        rule.setNoticeContentFormat(NoticeContentFormat.HTML);
        rule.setNoticeBodyTemplate("<p><b>{label}</b> の締め日は {date} です。</p>");

        NoticeTargetRow targetRow = NoticeTargetRow.builder()
                .targetKey("1")
                .targetDate(LocalDate.now().plusDays(3))
                .targetLabel("株式会社A")
                .build();

        Notice savedNotice = new Notice();
        savedNotice.setId(101L);

        String rendered = "<p><b>株式会社A</b> の締め日は " + targetRow.targetDate() + " です。</p>";

        when(ruleRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rule));

        when(noticeTargetResolver.resolve(rule))
                .thenReturn(List.of(targetRow));

        when(generatedRepository.existsByRuleCodeAndTargetTableNameAndTargetKeyAndTargetDateAndDeletedAtIsNull(
                rule.getRuleCode(),
                rule.getTargetTableName(),
                "1",
                targetRow.targetDate()
        )).thenReturn(false);

        when(noticeContentRenderer.render(
                NoticeContentFormat.HTML,
                rendered
        )).thenReturn(rendered);

        when(noticeRepository.save(any(Notice.class)))
                .thenReturn(savedNotice);

        NoticeGenerateResult result = service.generateByRuleId(1L);

        assertThat(result.generatedCount()).isEqualTo(1);

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(noticeCaptor.capture());

        Notice notice = noticeCaptor.getValue();

        assertThat(notice.getContentFormat()).isEqualTo(NoticeContentFormat.HTML);
        assertThat(notice.getContent()).isEqualTo(rendered);

        verify(noticeContentRenderer).render(
                NoticeContentFormat.HTML,
                rendered
        );
    }

    @SuppressWarnings("null")
    @Test
    void generateByRuleId_shouldSkip_whenTargetDoesNotMatchRule() {
        NoticeRule rule = validBeforeDaysRule();

        NoticeTargetRow targetRow = NoticeTargetRow.builder()
                .targetKey("1")
                .targetDate(LocalDate.now().plusDays(10))
                .targetLabel("株式会社A")
                .build();

        when(ruleRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rule));

        when(noticeTargetResolver.resolve(rule))
                .thenReturn(List.of(targetRow));

        NoticeGenerateResult result = service.generateByRuleId(1L);

        assertThat(result.generatedCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(1);

        verify(noticeRepository, never()).save(any());
        verify(generatedRepository, never()).save(any());
        verify(noticeContentRenderer, never()).render(any(), any());
    }

    @SuppressWarnings("null")
    @Test
    void generateByRuleId_shouldSkip_whenAlreadyGenerated() {
        NoticeRule rule = validBeforeDaysRule();

        NoticeTargetRow targetRow = NoticeTargetRow.builder()
                .targetKey("1")
                .targetDate(LocalDate.now().plusDays(3))
                .targetLabel("株式会社A")
                .build();

        when(ruleRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rule));

        when(noticeTargetResolver.resolve(rule))
                .thenReturn(List.of(targetRow));

        when(generatedRepository.existsByRuleCodeAndTargetTableNameAndTargetKeyAndTargetDateAndDeletedAtIsNull(
                rule.getRuleCode(),
                rule.getTargetTableName(),
                "1",
                targetRow.targetDate()
        )).thenReturn(true);

        NoticeGenerateResult result = service.generateByRuleId(1L);

        assertThat(result.generatedCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(1);

        verify(noticeRepository, never()).save(any());
        verify(generatedRepository, never()).save(any(NoticeGenerated.class));
        verify(noticeContentRenderer, never()).render(any(), any());
    }

    @SuppressWarnings("null")
    @Test
    void generateByRuleId_shouldSkip_whenTargetKeyIsNull() {
        NoticeRule rule = validBeforeDaysRule();

        NoticeTargetRow targetRow = NoticeTargetRow.builder()
                .targetKey(null)
                .targetDate(LocalDate.now().plusDays(3))
                .targetLabel("株式会社A")
                .build();

        when(ruleRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rule));

        when(noticeTargetResolver.resolve(rule))
                .thenReturn(List.of(targetRow));

        NoticeGenerateResult result = service.generateByRuleId(1L);

        assertThat(result.generatedCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(1);

        verify(noticeRepository, never()).save(any());
        verify(generatedRepository, never()).save(any());
        verify(noticeContentRenderer, never()).render(any(), any());
    }

    @SuppressWarnings("null")
    @Test
    void generateAll_shouldGenerateNoticesForActiveRules() {
        NoticeRule rule = validBeforeDaysRule();

        NoticeTargetRow targetRow = NoticeTargetRow.builder()
                .targetKey("1")
                .targetDate(LocalDate.now().plusDays(3))
                .targetLabel("株式会社A")
                .build();

        Notice savedNotice = new Notice();
        savedNotice.setId(100L);

        when(ruleRepository.findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc())
                .thenReturn(List.of(rule));

        when(noticeTargetResolver.resolve(rule))
                .thenReturn(List.of(targetRow));

        when(generatedRepository.existsByRuleCodeAndTargetTableNameAndTargetKeyAndTargetDateAndDeletedAtIsNull(
                rule.getRuleCode(),
                rule.getTargetTableName(),
                "1",
                targetRow.targetDate()
        )).thenReturn(false);

        when(noticeContentRenderer.render(
                NoticeContentFormat.PLAIN_TEXT,
                "株式会社A の締め日は " + targetRow.targetDate() + " です。"
        )).thenReturn("株式会社A の締め日は " + targetRow.targetDate() + " です。");

        when(noticeRepository.save(any(Notice.class)))
                .thenReturn(savedNotice);

        NoticeGenerateResult result = service.generateAll();

        assertThat(result.ruleCount()).isEqualTo(1);
        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.generatedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(0);

        verify(noticeRepository, times(1)).save(any(Notice.class));
        verify(generatedRepository, times(1)).save(any(NoticeGenerated.class));
    }

    private NoticeRule validBeforeDaysRule() {
        NoticeRule rule = new NoticeRule();
        rule.setRuleCode("CUSTOMER_CLOSING_NOTICE");
        rule.setRuleName("顧客締め日通知");
        rule.setTargetTableName("customers");
        rule.setTargetKeyColumnName("id");
        rule.setTargetDateColumnName("closing_date");
        rule.setTargetLabelColumnName("name");
        rule.setNoticeTitleTemplate("{label} の締め日が近づいています");
        rule.setNoticeBodyTemplate("{label} の締め日は {date} です。");
        rule.setNoticeContentFormat(NoticeContentFormat.PLAIN_TEXT);
        rule.setNoticeSeverity(NoticeSeverity.INFO);
        rule.setDateType(NoticeDateType.BEFORE_DAYS);
        rule.setDaysBefore(3);
        rule.setActiveFlag(true);
        return rule;
    }
}