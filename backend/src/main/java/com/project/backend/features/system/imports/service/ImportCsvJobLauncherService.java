package com.project.backend.features.system.imports.service;

import java.nio.file.Path;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import com.project.backend.features.system.imports.dto.ImportExecuteResult;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportCsvJobLauncherService {

    private final JobLauncher jobLauncher;
    private final Job csvImportJob;

    @SuppressWarnings("null")
    public ImportExecuteResult run(
            ImportTargetDefinition target,
            Path csvPath,
            String fileName
    ) {
        try {
            JobExecution execution = jobLauncher.run(
                    csvImportJob,
                    new JobParametersBuilder()
                            .addString("targetCode", target.targetCode())
                            .addString("filePath", csvPath.toAbsolutePath().toString())
                            .addString("charset", target.charset())
                            .addString("delimiter", target.delimiter())
                            .addLong("headerRowNumber", target.headerRowNumber().longValue())
                            .addLong("dataStartRowNumber", target.dataStartRowNumber().longValue())
                            .addLong("runId", System.currentTimeMillis())
                            .toJobParameters()
            );

            return ImportExecuteResult.builder()
                    .targetCode(target.targetCode())
                    .fileName(fileName)
                    .jobExecutionId(execution.getId())
                    .status(execution.getStatus().name())
                    .message("インポートを開始しました。")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("CSVインポートJobの起動に失敗しました。", e);
        }
    }
}