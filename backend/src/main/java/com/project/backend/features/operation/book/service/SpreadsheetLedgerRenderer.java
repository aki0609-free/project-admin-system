package com.project.backend.features.operation.book.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 帳票固有のSpreadsheet配置処理。
 *
 * <p>View、テンプレート、選択条件、保存処理は共通基盤が担当し、
 * 実装クラスはセル配置と固有の手入力復元だけを担当する。</p>
 */
public interface SpreadsheetLedgerRenderer {

    String rendererKey();

    JsonNode render(SpreadsheetLedgerRenderContext context);

    /**
     * S3上のSpreadsheetテンプレートを必要とするか。
     * 専用RendererがWorkbook全体を構築する場合はfalseを返す。
     */
    default boolean requiresTemplate() {
        return true;
    }

    /**
     * 固定配置Rendererは、カタログで許可された列をすべて取得する。
     */
    default boolean usesAllSourceColumns() {
        return true;
    }

    /**
     * テンプレート変数方式では1件以上のマッピングを必須とする。
     */
    default boolean requiresVariableMappings() {
        return false;
    }

    /**
     * 締め前の生成台帳を画面から編集できるか。
     */
    default boolean editableBeforeClosing() {
        return false;
    }

    /**
     * 請求締め後にも入金確認などの後続業務を編集できるか。
     */
    default boolean editableAfterMonthlyClosing() {
        return false;
    }

    /**
     * 同じ対象月を再生成したときに同じS3キーへ保存するか。
     */
    default boolean usesStableMonthlyPath() {
        return false;
    }

    /**
     * 再生成したWorkbookへ、既存Workbookの手入力値を引き継ぐ。
     */
    default void preserveManualInputs(
            JsonNode generated,
            JsonNode existing
    ) {
        // 手入力項目を持たないRendererは何もしない。
    }
}
