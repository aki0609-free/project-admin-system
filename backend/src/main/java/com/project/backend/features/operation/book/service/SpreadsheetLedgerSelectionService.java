package com.project.backend.features.operation.book.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.book.dto.SpreadsheetLedgerSelectionColumnResponse;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerSelectionOptionResponse;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerSelectionResponse;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.enums.ExcelBookGenerationUnit;
import com.project.backend.features.system.excelbook.enums.ExcelBookSelectionMode;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;
import com.project.backend.features.system.excelbook.service.ExcelBookDataSourceCatalogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpreadsheetLedgerSelectionService {

    private final ExcelBookMasterRepository masterRepository;
    private final ExcelBookDataSourceCatalogService catalogService;
    private final ExcelBookDataSourceRowQueryService rowQueryService;

    public SpreadsheetLedgerSelectionResponse find(
            String bookCode,
            String targetMonth
    ) {
        ExcelBookMaster master = masterRepository
                .findFirstByBookCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        bookCode
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "有効な台帳マスタが見つかりません: " + bookCode
                ));
        if (master.getSelectionMode() == ExcelBookSelectionMode.NONE) {
            return new SpreadsheetLedgerSelectionResponse(
                    ExcelBookSelectionMode.NONE,
                    null,
                    false,
                    ExcelBookGenerationUnit.ONE_FILE,
                    List.of(),
                    List.of()
            );
        }

        var catalog = catalogService.findRequired(
                master.getSelectionSourceName()
        );
        List<String> displayColumns = splitColumns(
                master.getSelectionDisplayColumns()
        );
        var allowedByName = catalog.getColumns().stream()
                .filter(column ->
                        column.isActiveFlag()
                                && column.getDeletedAt() == null
                )
                .collect(java.util.stream.Collectors.toMap(
                        column -> column.getColumnName(),
                        column -> column
                ));
        if (!allowedByName.containsKey(
                master.getSelectionValueColumn()
        )) {
            throw new IllegalStateException(
                    "選択値の項目がデータソースに存在しません: "
                            + master.getSelectionValueColumn()
            );
        }

        List<SpreadsheetLedgerSelectionColumnResponse> columns =
                displayColumns.stream().map(columnName -> {
                    var column = allowedByName.get(columnName);
                    if (column == null) {
                        throw new IllegalStateException(
                                "選択一覧の表示項目がデータソースに存在しません: "
                                        + columnName
                        );
                    }
                    return new SpreadsheetLedgerSelectionColumnResponse(
                            column.getColumnName(),
                            column.getDisplayName(),
                            column.getDataType(),
                            column.getOrderNo()
                    );
                }).toList();

        List<Map<String, Object>> rows = rowQueryService.findAllRows(
                catalog,
                targetMonth
        );
        LinkedHashMap<String, SpreadsheetLedgerSelectionOptionResponse>
                uniqueOptions = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object rawValue = row.get(master.getSelectionValueColumn());
            if (rawValue == null) {
                continue;
            }
            String value = rawValue.toString();
            LinkedHashMap<String, Object> displayValues =
                    new LinkedHashMap<>();
            for (String column : displayColumns) {
                displayValues.put(column, row.get(column));
            }
            uniqueOptions.putIfAbsent(
                    value,
                    new SpreadsheetLedgerSelectionOptionResponse(
                            value,
                            displayValues
                    )
            );
        }

        return new SpreadsheetLedgerSelectionResponse(
                master.getSelectionMode(),
                master.getSelectionValueColumn(),
                Boolean.TRUE.equals(master.getAllowSelectAll()),
                master.getGenerationUnit(),
                columns,
                List.copyOf(uniqueOptions.values())
        );
    }

    private List<String> splitColumns(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> columns =
                java.util.Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(column -> !column.isEmpty())
                        .collect(java.util.stream.Collectors.toCollection(
                                LinkedHashSet::new
                        ));
        return List.copyOf(columns);
    }
}
