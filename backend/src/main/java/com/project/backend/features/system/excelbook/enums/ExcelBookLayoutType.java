package com.project.backend.features.system.excelbook.enums;

/**
 * Spreadsheetへデータを配置する方式。
 *
 * <p>業務別レイアウトはこの列で切り替え、汎用の台帳生成基盤と
 * セル配置ロジックを分離する。</p>
 */
public enum ExcelBookLayoutType {
    REPEATING_ROW,
    MONTHLY_SUMMARY,
    DEDICATED
}
