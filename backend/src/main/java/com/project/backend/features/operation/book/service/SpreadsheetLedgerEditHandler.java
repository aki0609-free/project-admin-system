package com.project.backend.features.operation.book.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Spreadsheetの編集内容を業務DBへ反映する帳票固有ハンドラー。
 */
public interface SpreadsheetLedgerEditHandler {

    String rendererKey();

    void apply(String targetMonth, JsonNode workbook);
}
