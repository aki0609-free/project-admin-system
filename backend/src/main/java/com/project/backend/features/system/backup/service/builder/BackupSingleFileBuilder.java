package com.project.backend.features.system.backup.service.builder;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.app.file.service.CsvFileWriter;
import com.project.backend.features.system.backup.dto.BackupColumnDefinition;
import com.project.backend.features.system.backup.dto.BackupTargetDefinition;
import com.project.backend.features.system.backup.dto.SingleBackupFile;
import com.project.backend.features.system.backup.service.BackupDefinitionService;
import com.project.backend.features.system.backup.service.fetcher.BackupDataFetcher;
import com.project.backend.features.system.backup.service.resolver.BackupExportColumnResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupSingleFileBuilder {

    private final BackupDefinitionService backupDefinitionService;
    private final BackupExportColumnResolver exportColumnResolver;
    private final BackupDataFetcher dataFetcher;
    private final CsvFileWriter csvFileWriter;
    private final BackupSqlBuilder sqlBuilder;
    private final BackupFileNameBuilder fileNameBuilder;
    private final BackupCsvDataBuilder csvDataBuilder;

    public SingleBackupFile build(String targetCode) {
        BackupTargetDefinition target =
                backupDefinitionService.getBackupTargetDefinition(targetCode);

        List<BackupColumnDefinition> columns =
                exportColumnResolver.resolve(target);

        String sql = sqlBuilder.buildSelectSql(
                target.tableName(),
                columns
        );

        List<Map<String, Object>> rows =
                dataFetcher.fetch(sql);

        List<String> columnKeys = columns.stream()
                .map(BackupColumnDefinition::columnName)
                .toList();

        List<String> headerNames = columns.stream()
                .map(BackupColumnDefinition::csvHeaderName)
                .toList();

        byte[] csv = csvFileWriter.write(
                csvDataBuilder.build(
                        rows,
                        columns
                ),
                columnKeys,
                headerNames,
                target.includeHeader()
        );

        return SingleBackupFile.builder()
                .targetCode(target.targetCode())
                .targetName(target.targetName())
                .fileName(fileNameBuilder.buildCsvFileName(
                        target.targetCode(),
                        target.fileNamePattern()
                ))
                .data(csv)
                .zipRequired(target.zipRequired())
                .outputMode(target.outputMode())
                .outputDir(target.outputDir())
                .build();
    }
}