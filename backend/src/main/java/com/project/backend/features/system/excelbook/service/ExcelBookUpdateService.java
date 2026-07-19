package com.project.backend.features.system.excelbook.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;
import com.project.backend.features.system.excelbook.service.parser.ExcelTemplateParser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelBookUpdateService {

    private final ExcelBookMasterRepository repository;
    private final SnapshotMapQueryService snapshotMapQueryService;
    private final ExcelTemplateParser excelTemplateParser;

    public Path updateBook(
            String bookCode,
            String targetMonth
    ) {
        if (bookCode == null || bookCode.isBlank()) {
            throw new IllegalArgumentException("bookCode は必須です。");
        }

        if (targetMonth == null || targetMonth.isBlank()) {
            throw new IllegalArgumentException("targetMonth は必須です。");
        }

        ExcelBookMaster master = repository
                .findFirstByBookCodeAndActiveFlagTrueAndDeletedAtIsNull(bookCode)
                .orElseThrow(() -> new IllegalArgumentException("Excel台帳マスタが見つかりません: " + bookCode));

        if (master.getSourceType() != ExcelBookSourceType.SNAPSHOT) {
            throw new UnsupportedOperationException("現在は SNAPSHOT のみ対応しています。");
        }

        List<Map<String, Object>> rows =
                snapshotMapQueryService.findRows(master.getSourceName(), targetMonth);

        Map<String, Object> context =
                snapshotMapQueryService.buildContext(targetMonth, rows);

        Path templatePath = Path.of(master.getTemplateFilePath());
        Path outputPath = Path.of(master.getOutputFilePath());

        excelTemplateParser.writeToBook(
                templatePath,
                outputPath,
                master.getTemplateSheetName(),
                targetMonth,
                context
        );

        return outputPath;
    }
}