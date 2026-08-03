package com.project.backend.features.operation.book.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/**
 * ${rows.*} の明細行を繰り返す汎用Renderer。
 */
@Component
@RequiredArgsConstructor
public class RepeatingRowSpreadsheetRenderer
        implements SpreadsheetLedgerRenderer {

    public static final String KEY = "REPEATING_ROW";

    private final SpreadsheetWorkbookTemplateExpander templateExpander;

    @Override
    public String rendererKey() {
        return KEY;
    }

    @Override
    public JsonNode render(SpreadsheetLedgerRenderContext context) {
        return templateExpander.expand(
                context.template(),
                context.master(),
                context.sourceRows(),
                context.targetMonth(),
                context.generatedAt()
        );
    }

    @Override
    public boolean usesAllSourceColumns() {
        return false;
    }

    @Override
    public boolean requiresVariableMappings() {
        return true;
    }
}
