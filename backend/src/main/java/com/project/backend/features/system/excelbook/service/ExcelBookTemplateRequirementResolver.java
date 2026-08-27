package com.project.backend.features.system.excelbook.service;

/**
 * 台帳RendererがSpreadsheetテンプレートを必要とするかを解決する境界。
 *
 * <p>台帳マスタ管理側が個別Rendererへ直接依存しないためのSPI。</p>
 */
public interface ExcelBookTemplateRequirementResolver {

    boolean requiresTemplate(String rendererKey);

    boolean requiresVariableMappings(String rendererKey);
}
