package com.project.backend.features.system.notice.service.generator;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeSourceType;
import com.project.backend.features.dashboard.repository.NoticeRepository;
import com.project.backend.features.dashboard.service.renderer.NoticeContentRenderer;
import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeGenerated;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.GenerateResult;
import com.project.backend.features.system.notice.repository.NoticeGeneratedRepository;
import com.project.backend.features.system.notice.utils.NoticeUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeGenerator {

    private final NoticeGeneratedRepository generatedRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeContentRenderer noticeContentRenderer;

    @SuppressWarnings("null")
    public GenerateResult generate(
            NoticeRule rule,
            NoticeTargetRow row,
            LocalDate today
    ) {
        String targetKey = row.targetKey();
        LocalDate targetDate = row.targetDate();
        String label = row.targetLabel();

        if (targetKey == null || targetDate == null) {
            return GenerateResult.SKIPPED;
        }

        if (!NoticeUtils.matches(rule, targetDate, today)) {
            return GenerateResult.SKIPPED;
        }

        if (existsGenerated(rule, targetKey, targetDate)) {
            return GenerateResult.SKIPPED;
        }

        Notice saved =
                noticeRepository.save(
                        buildNotice(
                                rule,
                                targetKey,
                                targetDate,
                                label,
                                today
                        )
                );

        generatedRepository.save(
                buildGenerated(
                        rule,
                        targetKey,
                        targetDate,
                        saved.getId()
                )
        );

        return GenerateResult.GENERATED;
    }

    private boolean existsGenerated(
            NoticeRule rule,
            String targetKey,
            LocalDate targetDate
    ) {
        return generatedRepository
                .existsByRuleCodeAndTargetTableNameAndTargetKeyAndTargetDateAndDeletedAtIsNull(
                        rule.getRuleCode(),
                        rule.getTargetTableName(),
                        targetKey,
                        targetDate
                );
    }

    private Notice buildNotice(
            NoticeRule rule,
            String targetKey,
            LocalDate targetDate,
            String label,
            LocalDate today
    ) {
        NoticeContentFormat contentFormat =
                rule.getNoticeContentFormat() != null
                        ? rule.getNoticeContentFormat()
                        : NoticeContentFormat.PLAIN_TEXT;

        String renderedContent =
                NoticeUtils.applyTemplate(
                        rule.getNoticeBodyTemplate(),
                        label,
                        targetDate,
                        targetKey
                );

        Notice notice = new Notice();

        notice.setTitle(
                NoticeUtils.applyTemplate(
                        rule.getNoticeTitleTemplate(),
                        label,
                        targetDate,
                        targetKey
                )
        );

        notice.setContent(
                noticeContentRenderer.render(
                        contentFormat,
                        renderedContent
                )
        );

        notice.setStartDate(today);
        notice.setEndDate(targetDate);
        notice.setType(NoticeUtils.toNoticeType(rule));
        notice.setColor(NoticeUtils.toColor(rule));
        notice.setContentFormat(contentFormat);
        notice.setSourceType(NoticeSourceType.AUTO);
        notice.setSourceRuleCode(rule.getRuleCode());
        notice.setPinnedFlag(false);
        notice.setActiveFlag(true);

        return notice;
    }

    private NoticeGenerated buildGenerated(
            NoticeRule rule,
            String targetKey,
            LocalDate targetDate,
            Long noticeId
    ) {
        NoticeGenerated generated = new NoticeGenerated();

        generated.setRuleCode(rule.getRuleCode());
        generated.setTargetTableName(rule.getTargetTableName());
        generated.setTargetKey(targetKey);
        generated.setTargetDate(targetDate);
        generated.setGeneratedNoticeId(noticeId);

        return generated;
    }
}