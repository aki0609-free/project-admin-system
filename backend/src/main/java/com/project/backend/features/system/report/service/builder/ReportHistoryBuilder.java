package com.project.backend.features.system.report.service.builder;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.entity.ReportHistory;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportHistoryStatus;
import com.project.backend.features.system.report.service.converter.ReportRequestParamsSanitizer;
import com.project.backend.features.system.report.service.resolver.ReportHistoryMessageResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportHistoryBuilder {

    private final ObjectMapper objectMapper;
    private final ReportRequestParamsSanitizer sanitizer;
    private final ReportHistoryMessageResolver messageResolver;

    public ReportHistory build(
            ReportMaster reportMaster,
            String executionId,
            Map<String, Object> requestParams,
            ReportHistoryStatus status,
            String notes,
            ReportStoredFile storedFile
    ) {
        try {
            ReportHistory history = new ReportHistory();

            history.setReportMaster(reportMaster);
            history.setFileName(storedFile != null ? storedFile.fileName() : reportMaster.getFileName());
            history.setOutputFormat(reportMaster.getOutputFormat());
            history.setStatus(status);
            history.setExecutedBy("system");
            history.setNotes(messageResolver.notes(notes, executionId));
            history.setRequestParamsJson(
                    objectMapper.writeValueAsString(sanitizer.sanitize(requestParams))
            );

            if (storedFile != null) {
                history.setStorageType(storedFile.storageType());
                history.setStoredFileKey(storedFile.fileKey());
                history.setStoredFileName(storedFile.fileName());
                history.setMimeType(storedFile.contentType());
                history.setFileSize(storedFile.fileSize());
            }

            return history;

        } catch (Exception e) {
            throw new RuntimeException("帳票履歴Entityの生成に失敗しました。", e);
        }
    }
}