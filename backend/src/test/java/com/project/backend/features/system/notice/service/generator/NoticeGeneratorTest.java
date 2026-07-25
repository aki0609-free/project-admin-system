package com.project.backend.features.system.notice.service.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeSourceType;
import com.project.backend.features.dashboard.enums.NoticeType;
import com.project.backend.features.dashboard.repository.NoticeRepository;
import com.project.backend.features.dashboard.service.renderer.NoticeContentRenderer;
import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeGenerated;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.GenerateResult;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.enums.NoticeSeverity;
import com.project.backend.features.system.notice.repository.NoticeGeneratedRepository;

class NoticeGeneratorTest {

    private static final LocalDate TODAY =
            LocalDate.of(2026, 7, 25);

    private NoticeGeneratedRepository generatedRepository;
    private NoticeRepository noticeRepository;
    private NoticeContentRenderer contentRenderer;
    private NoticeGenerator generator;

    @BeforeEach
    void setUp() {
        generatedRepository =
                mock(NoticeGeneratedRepository.class);
        noticeRepository = mock(NoticeRepository.class);
        contentRenderer = mock(NoticeContentRenderer.class);
        generator = new NoticeGenerator(
                generatedRepository,
                noticeRepository,
                contentRenderer
        );
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void generate_shouldCreateNoticeAndGenerationHistory() {
        NoticeRule rule = validRule();
        LocalDate targetDate = TODAY.plusDays(3);
        NoticeTargetRow row = target("1", targetDate);
        String body = "株式会社A の締め日は "
                + targetDate
                + " です。";

        when(generatedRepository
                .existsByTenantIdAndRuleCodeAndTargetTableNameAndTargetKeyAndTargetDateAndDeletedAtIsNull(
                        "tenant-a",
                        rule.getRuleCode(),
                        rule.getTargetTableName(),
                        "1",
                        targetDate
                )).thenReturn(false);
        when(contentRenderer.render(
                NoticeContentFormat.PLAIN_TEXT,
                body
        )).thenReturn(body);

        Notice saved = new Notice();
        saved.setId(100L);
        when(noticeRepository.save(any(Notice.class)))
                .thenReturn(saved);

        GenerateResult result =
                generator.generate(rule, row, TODAY);

        assertThat(result).isEqualTo(GenerateResult.GENERATED);

        ArgumentCaptor<Notice> noticeCaptor =
                ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(noticeCaptor.capture());
        Notice notice = noticeCaptor.getValue();

        assertThat(notice.getTitle())
                .isEqualTo("株式会社A の締め日が近づいています");
        assertThat(notice.getContent()).isEqualTo(body);
        assertThat(notice.getStartDate()).isEqualTo(TODAY);
        assertThat(notice.getEndDate()).isEqualTo(targetDate);
        assertThat(notice.getType()).isEqualTo(NoticeType.INFO);
        assertThat(notice.getColor()).isEqualTo("blue");
        assertThat(notice.getSourceType())
                .isEqualTo(NoticeSourceType.AUTO);

        ArgumentCaptor<NoticeGenerated> generatedCaptor =
                ArgumentCaptor.forClass(NoticeGenerated.class);
        verify(generatedRepository).save(
                generatedCaptor.capture()
        );
        assertThat(generatedCaptor.getValue()
                .getGeneratedNoticeId()).isEqualTo(100L);
    }

    @Test
    void generate_shouldSkipMissingKey() {
        GenerateResult result = generator.generate(
                validRule(),
                target(null, TODAY.plusDays(3)),
                TODAY
        );

        assertThat(result).isEqualTo(GenerateResult.SKIPPED);
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void generate_shouldSkipNonMatchingDate() {
        GenerateResult result = generator.generate(
                validRule(),
                target("1", TODAY.plusDays(10)),
                TODAY
        );

        assertThat(result).isEqualTo(GenerateResult.SKIPPED);
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void generate_shouldSkipTenantDuplicate() {
        NoticeRule rule = validRule();
        LocalDate targetDate = TODAY.plusDays(3);

        when(generatedRepository
                .existsByTenantIdAndRuleCodeAndTargetTableNameAndTargetKeyAndTargetDateAndDeletedAtIsNull(
                        "tenant-a",
                        rule.getRuleCode(),
                        rule.getTargetTableName(),
                        "1",
                        targetDate
                )).thenReturn(true);

        GenerateResult result = generator.generate(
                rule,
                target("1", targetDate),
                TODAY
        );

        assertThat(result).isEqualTo(GenerateResult.SKIPPED);
        verify(noticeRepository, never()).save(any());
    }

    private NoticeRule validRule() {
        NoticeRule rule = new NoticeRule();
        rule.setRuleCode("CUSTOMER_CLOSING_NOTICE");
        rule.setRuleName("顧客締め日通知");
        rule.setTargetTableName("customers");
        rule.setNoticeTitleTemplate(
                "{label} の締め日が近づいています"
        );
        rule.setNoticeBodyTemplate(
                "{label} の締め日は {date} です。"
        );
        rule.setNoticeContentFormat(
                NoticeContentFormat.PLAIN_TEXT
        );
        rule.setNoticeSeverity(NoticeSeverity.INFO);
        rule.setDateType(NoticeDateType.BEFORE_DAYS);
        rule.setDaysBefore(3);
        return rule;
    }

    private NoticeTargetRow target(
            String key,
            LocalDate targetDate
    ) {
        return NoticeTargetRow.builder()
                .targetKey(key)
                .targetDate(targetDate)
                .targetLabel("株式会社A")
                .build();
    }
}
