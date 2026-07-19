package com.project.backend.features.system.report.service.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.report.dto.ReportRecipientFileProcessResult;
import com.project.backend.features.system.report.dto.ReportRecipientGenerationSummary;
import com.project.backend.features.system.report.dto.ReportRecipientOutputGroup;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportOutputFileStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportRecipientFileGenerationService {

    private final ReportRecipientOutputGrouper outputGrouper;
    private final ReportRecipientFileProcessor fileProcessor;

    public ReportRecipientGenerationSummary generate(
            ReportMaster reportMaster,
            String executionId,
            List<Map<String, Object>> rows
    ) {
        List<ReportRecipientOutputGroup> groups = outputGrouper.group(rows);

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        int groupIndex = 0;
        Set<String> mailTypes = new LinkedHashSet<>();

        for (ReportRecipientOutputGroup group : groups) {
            groupIndex++;

            try {
                ReportRecipientFileProcessResult result = fileProcessor.process(
                        reportMaster,
                        executionId,
                        group
                );

                if (result.skipped()) {
                    skippedCount++;
                    continue;
                }

                if (result.status() == ReportOutputFileStatus.CREATED) {
                    successCount++;
                    mailTypes.add(result.mailType());
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error(
                        "個人別帳票の生成に失敗しました。 executionId={}, groupIndex={}",
                        executionId,
                        groupIndex,
                        e
                );
            }
        }

        return new ReportRecipientGenerationSummary(
                groups.size(),
                successCount,
                failedCount,
                skippedCount,
                Set.copyOf(mailTypes)
        );
    }
}
