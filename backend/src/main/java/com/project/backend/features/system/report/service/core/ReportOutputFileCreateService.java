package com.project.backend.features.system.report.service.core;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportOutputFile;
import com.project.backend.features.system.report.repository.ReportOutputFileRepository;
import com.project.backend.features.system.report.service.api.OutputTableQueryService;
import com.project.backend.features.system.report.service.builder.ReportOutputFileBuilder;
import com.project.backend.features.system.report.service.validation.ReportOutputFileDuplicateChecker;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportOutputFileCreateService {

    private final ReportOutputFileRepository repository;
    private final OutputTableQueryService outputTableQueryService;
    private final ReportOutputFileBuilder builder;
    private final ReportOutputFileDuplicateChecker duplicateChecker;

    @Transactional
    public int create(
            ReportMaster reportMaster,
            String executionId,
            ReportStoredFile storedFile
    ) {
        List<Map<String, Object>> rows =
                outputTableQueryService.findByExecutionId(
                        reportMaster.resolveOutputTableName(),
                        executionId
                );

        int count = 0;

        for (Map<String, Object> row : rows) {
            ReportOutputFile file =
                    builder.build(
                            reportMaster,
                            executionId,
                            storedFile,
                            row
                    );

            if (file == null) {
                continue;
            }

            if (duplicateChecker.isDuplicate(file)) {
                continue;
            }

            repository.save(file);
            count++;
        }

        return count;
    }
}