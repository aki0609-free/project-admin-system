package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

class SpreadsheetLedgerRendererRegistryTest {

    @Test
    void findRequired_shouldResolveRendererByKey() {
        SpreadsheetLedgerRenderer renderer = renderer("LEDGER_V1");
        var registry = new SpreadsheetLedgerRendererRegistry(
                List.of(renderer)
        );

        assertThat(registry.findRequired("LEDGER_V1"))
                .isSameAs(renderer);
    }

    @Test
    void constructor_shouldRejectDuplicateKey() {
        assertThatThrownBy(() ->
                new SpreadsheetLedgerRendererRegistry(
                        List.of(
                                renderer("DUPLICATE"),
                                renderer("DUPLICATE")
                        )
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重複");
    }

    private SpreadsheetLedgerRenderer renderer(String key) {
        SpreadsheetLedgerRenderer renderer = mock(
                SpreadsheetLedgerRenderer.class
        );
        when(renderer.rendererKey()).thenReturn(key);
        return renderer;
    }
}
