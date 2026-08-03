package com.project.backend.features.operation.book.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SpreadsheetLedgerEditHandlerRegistry {

    private final Map<String, SpreadsheetLedgerEditHandler> handlers;

    public SpreadsheetLedgerEditHandlerRegistry(
            List<SpreadsheetLedgerEditHandler> handlerList
    ) {
        Map<String, SpreadsheetLedgerEditHandler> registered =
                new LinkedHashMap<>();
        for (SpreadsheetLedgerEditHandler handler : handlerList) {
            String key = handler.rendererKey();
            if (!StringUtils.hasText(key)) {
                throw new IllegalStateException(
                        "Spreadsheet Edit HandlerのrendererKeyが未設定です。"
                );
            }
            if (registered.putIfAbsent(key, handler) != null) {
                throw new IllegalStateException(
                        "Spreadsheet Edit Handlerが重複しています: " + key
                );
            }
        }
        handlers = Map.copyOf(registered);
    }

    public Optional<SpreadsheetLedgerEditHandler> find(
            String rendererKey
    ) {
        return Optional.ofNullable(handlers.get(rendererKey));
    }
}
