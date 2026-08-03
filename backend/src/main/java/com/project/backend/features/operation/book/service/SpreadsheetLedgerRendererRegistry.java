package com.project.backend.features.operation.book.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * rendererKeyから帳票固有Rendererを解決するRegistry。
 */
@Component
public class SpreadsheetLedgerRendererRegistry {

    private final Map<String, SpreadsheetLedgerRenderer> renderers;

    public SpreadsheetLedgerRendererRegistry(
            List<SpreadsheetLedgerRenderer> rendererList
    ) {
        Map<String, SpreadsheetLedgerRenderer> registered =
                new LinkedHashMap<>();
        for (SpreadsheetLedgerRenderer renderer : rendererList) {
            String key = renderer.rendererKey();
            if (!StringUtils.hasText(key)) {
                throw new IllegalStateException(
                        "rendererKeyが未設定のRendererがあります。"
                );
            }
            SpreadsheetLedgerRenderer duplicate =
                    registered.putIfAbsent(key, renderer);
            if (duplicate != null) {
                throw new IllegalStateException(
                        "rendererKeyが重複しています: " + key
                );
            }
        }
        this.renderers = Map.copyOf(registered);
    }

    public SpreadsheetLedgerRenderer findRequired(String rendererKey) {
        if (!StringUtils.hasText(rendererKey)) {
            throw new IllegalArgumentException(
                    "rendererKeyは必須です。"
            );
        }
        SpreadsheetLedgerRenderer renderer = renderers.get(rendererKey);
        if (renderer == null) {
            throw new IllegalArgumentException(
                    "登録されていないSpreadsheet Rendererです: "
                            + rendererKey
            );
        }
        return renderer;
    }

    public List<String> keys() {
        return renderers.keySet().stream().sorted().toList();
    }
}
