package com.project.backend.features.system.notice.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.notice.dto.NoticeGenerateResult;
import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.GenerateResult;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;
import com.project.backend.features.system.notice.service.generator.NoticeGenerator;
import com.project.backend.features.system.notice.service.resolver.NoticeTargetResolverDispatcher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeAutoGenerateService {

    private final NoticeRuleRepository ruleRepository;
    private final NoticeTargetResolverDispatcher noticeTargetResolver;
    private final NoticeGenerator noticeGenerator;

    @Transactional
    public NoticeGenerateResult generateAll() {
        List<NoticeRule> rules =
                ruleRepository.findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

        NoticeGenerateCounter counter =
                new NoticeGenerateCounter();

        LocalDate today =
                LocalDate.now();

        for (NoticeRule rule : rules) {
            List<NoticeTargetRow> rows =
                    noticeTargetResolver.resolve(rule);

            counter.addTargetCount(rows.size());

            generateRows(
                    rule,
                    rows,
                    today,
                    counter
            );
        }

        return NoticeGenerateResult.builder()
                .ruleCount(rules.size())
                .targetCount(counter.targetCount)
                .generatedCount(counter.generatedCount)
                .skippedCount(counter.skippedCount)
                .message("Notice自動生成が完了しました。")
                .build();
    }

    @Transactional
    public NoticeGenerateResult generateByRuleId(
            Long ruleId
    ) {
        NoticeRule rule =
                ruleRepository.findByIdAndDeletedAtIsNull(ruleId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "NoticeRuleが見つかりません。 id=" + ruleId
                                )
                        );

        List<NoticeTargetRow> rows =
                noticeTargetResolver.resolve(rule);

        NoticeGenerateCounter counter =
                new NoticeGenerateCounter();

        LocalDate today =
                LocalDate.now();

        counter.addTargetCount(rows.size());

        generateRows(
                rule,
                rows,
                today,
                counter
        );

        return NoticeGenerateResult.builder()
                .ruleCount(1)
                .targetCount(counter.targetCount)
                .generatedCount(counter.generatedCount)
                .skippedCount(counter.skippedCount)
                .message("NoticeRule単体生成が完了しました。")
                .build();
    }

    private void generateRows(
            NoticeRule rule,
            List<NoticeTargetRow> rows,
            LocalDate today,
            NoticeGenerateCounter counter
    ) {
        for (NoticeTargetRow row : rows) {
            GenerateResult result =
                    noticeGenerator.generate(
                            rule,
                            row,
                            today
                    );

            if (result == GenerateResult.GENERATED) {
                counter.generated();
            } else {
                counter.skipped();
            }
        }
    }

    private static class NoticeGenerateCounter {

        private int targetCount;
        private int generatedCount;
        private int skippedCount;

        private void addTargetCount(
                int count
        ) {
            targetCount += count;
        }

        private void generated() {
            generatedCount++;
        }

        private void skipped() {
            skippedCount++;
        }
    }
}