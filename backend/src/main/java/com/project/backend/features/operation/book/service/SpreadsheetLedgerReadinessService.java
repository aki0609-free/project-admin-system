package com.project.backend.features.operation.book.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.enums.ExcelBookGenerationUnit;
import com.project.backend.features.system.excelbook.enums.ExcelBookSelectionMode;
import com.project.backend.features.system.excelbook.service.ExcelBookDataSourceCatalogService;
import com.project.backend.features.system.excelbook.service.SpreadsheetTemplateService;

import lombok.RequiredArgsConstructor;

/**
 * システムメニューで登録した台帳定義が、締めメニューから生成できる状態かを判定する。
 */
@Service
@RequiredArgsConstructor
public class SpreadsheetLedgerReadinessService {

    private final SpreadsheetLedgerRendererRegistry rendererRegistry;
    private final ExcelBookDataSourceCatalogService catalogService;
    private final SpreadsheetTemplateService templateService;
    private final MonthlyClosingOutputDefinitionRepository
            closingOutputRepository;

    public SpreadsheetLedgerReadiness assess(ExcelBookMaster master) {
        List<String> issues = new ArrayList<>();
        SpreadsheetLedgerRenderer renderer = findRenderer(master, issues);
        ExcelBookDataSourceCatalog sourceCatalog = findCatalog(
                master.getSourceName(),
                "データソース",
                issues
        );

        if (renderer != null && sourceCatalog != null) {
            validateVariableMappings(master, renderer, sourceCatalog, issues);
        }
        validateSelection(master, issues);

        boolean templateRequired = renderer != null
                && renderer.requiresTemplate();
        boolean templateConfigured = false;
        if (templateRequired) {
            try {
                templateConfigured = templateService.find(master.getId())
                        .workbook() != null;
                if (!templateConfigured) {
                    issues.add("Spreadsheetテンプレートが未登録です。");
                }
            } catch (RuntimeException exception) {
                issues.add("Spreadsheetテンプレートを確認できません。");
            }
        }

        boolean monthlyClosingConfigured = closingOutputRepository
                .findByTenantIdAndOutputTypeAndOutputCodeAndDeletedAtIsNull(
                        master.getTenantId(),
                        MonthlyClosingOutputType.LEDGER,
                        master.getBookCode()
                )
                .filter(definition ->
                        Boolean.TRUE.equals(definition.getActiveFlag())
                )
                .isPresent();

        return new SpreadsheetLedgerReadiness(
                issues.isEmpty(),
                templateRequired,
                templateConfigured,
                monthlyClosingConfigured,
                List.copyOf(issues)
        );
    }

    public void requireGenerationReady(ExcelBookMaster master) {
        SpreadsheetLedgerReadiness readiness = assess(master);
        if (!readiness.generationReady()) {
            throw new IllegalStateException(
                    "台帳設定が未完了です: "
                            + String.join(" / ", readiness.issues())
            );
        }
    }

    private SpreadsheetLedgerRenderer findRenderer(
            ExcelBookMaster master,
            List<String> issues
    ) {
        String key = StringUtils.hasText(master.getRendererKey())
                ? master.getRendererKey()
                : master.getLayoutType().name();
        try {
            return rendererRegistry.findRequired(key);
        } catch (RuntimeException exception) {
            issues.add("描画方式が登録されていません: " + key);
            return null;
        }
    }

    private ExcelBookDataSourceCatalog findCatalog(
            String sourceCode,
            String label,
            List<String> issues
    ) {
        if (!StringUtils.hasText(sourceCode)) {
            issues.add(label + "が未設定です。");
            return null;
        }
        try {
            return catalogService.findRequired(sourceCode);
        } catch (RuntimeException exception) {
            issues.add(label + "が利用できません: " + sourceCode);
            return null;
        }
    }

    private void validateVariableMappings(
            ExcelBookMaster master,
            SpreadsheetLedgerRenderer renderer,
            ExcelBookDataSourceCatalog catalog,
            List<String> issues
    ) {
        if (renderer.requiresVariableMappings()
                && master.getVariableMappings().isEmpty()) {
            issues.add("テンプレート変数が未設定です。");
            return;
        }
        Set<String> allowedColumns = activeColumns(catalog);
        master.getVariableMappings().stream()
                .filter(mapping -> mapping.getDeletedAt() == null)
                .filter(mapping ->
                        !allowedColumns.contains(mapping.getSourceColumn())
                )
                .forEach(mapping -> issues.add(
                        "テンプレート変数の参照項目が利用できません: "
                                + mapping.getSourceColumn()
                ));
    }

    private void validateSelection(
            ExcelBookMaster master,
            List<String> issues
    ) {
        if (master.getSelectionMode() == ExcelBookSelectionMode.NONE) {
            return;
        }
        if (master.getGenerationUnit()
                != ExcelBookGenerationUnit.FILE_PER_SELECTION) {
            issues.add("V1の対象選択型は「対象ごとに1ファイル」で設定してください。");
        }
        ExcelBookDataSourceCatalog catalog = findCatalog(
                master.getSelectionSourceName(),
                "選択一覧データソース",
                issues
        );
        if (catalog == null) {
            return;
        }
        Set<String> allowedColumns = activeColumns(catalog);
        if (!allowedColumns.contains(master.getSelectionValueColumn())) {
            issues.add("選択値の項目が利用できません: "
                    + master.getSelectionValueColumn());
        }
        splitColumns(master.getSelectionDisplayColumns()).stream()
                .filter(column -> !allowedColumns.contains(column))
                .forEach(column -> issues.add(
                        "選択一覧の表示項目が利用できません: " + column
                ));
    }

    private Set<String> activeColumns(ExcelBookDataSourceCatalog catalog) {
        Set<String> result = new HashSet<>();
        catalog.getColumns().stream()
                .filter(column ->
                        column.isActiveFlag()
                                && column.getDeletedAt() == null
                )
                .map(column -> column.getColumnName())
                .forEach(result::add);
        return result;
    }

    private List<String> splitColumns(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    public record SpreadsheetLedgerReadiness(
            boolean generationReady,
            boolean templateRequired,
            boolean templateConfigured,
            boolean monthlyClosingConfigured,
            List<String> issues
    ) {
    }
}
