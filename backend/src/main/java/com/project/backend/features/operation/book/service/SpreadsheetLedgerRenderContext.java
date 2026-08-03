package com.project.backend.features.operation.book.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;

/**
 * Spreadsheet台帳Rendererへ渡す共通入力。
 *
 * <p>帳票固有Rendererが生成ServiceやS3へ依存しないように、描画に
 * 必要な値だけをまとめる。</p>
 */
public record SpreadsheetLedgerRenderContext(
        JsonNode template,
        ExcelBookMaster master,
        List<Map<String, Object>> sourceRows,
        String targetMonth,
        Instant generatedAt,
        Map<String, Object> parameters
) {
    public SpreadsheetLedgerRenderContext {
        sourceRows = List.copyOf(sourceRows);
        parameters = Map.copyOf(parameters);
    }

    public SpreadsheetLedgerRenderContext(
            JsonNode template,
            ExcelBookMaster master,
            List<Map<String, Object>> sourceRows,
            String targetMonth,
            Instant generatedAt
    ) {
        this(
                template,
                master,
                sourceRows,
                targetMonth,
                generatedAt,
                Map.of()
        );
    }
}
