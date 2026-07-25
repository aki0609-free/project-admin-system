package com.project.backend.features.admin.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.features.admin.document.enums.DocumentArea;

class DocumentStorageKeyResolverTest {

    private DocumentStorageKeyResolver resolver;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        resolver = new DocumentStorageKeyResolver(properties);
    }

    @Test
    void resolveAreaRoot_shouldUseConfiguredDocumentFolders() {
        assertThat(resolver.resolveAreaRoot(DocumentArea.GENERAL))
                .isEqualTo("documents/general");
        assertThat(resolver.resolveAreaRoot(DocumentArea.GENERATED_REPORTS))
                .isEqualTo("documents/generated-reports");
        assertThat(resolver.resolveAreaRoot(DocumentArea.BACKUPS))
                .isEqualTo("documents/backups");
        assertThat(resolver.resolveAreaRoot(DocumentArea.TEMPLATES))
                .isEqualTo("documents/templates");
    }

    @Test
    void resolve_shouldNormalizeSeparatorsWithoutEscapingArea() {
        assertThat(resolver.resolve(
                DocumentArea.GENERAL,
                "/contracts/2026/sample.pdf/"
        )).isEqualTo("documents/general/contracts/2026/sample.pdf");
    }

    @Test
    void resolve_shouldRejectParentTraversal() {
        assertThatThrownBy(() ->
                resolver.resolve(
                        DocumentArea.GENERAL,
                        "../backups/report.pdf"
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("相対パス");
    }

    @Test
    void resolve_shouldRejectControlCharacters() {
        assertThatThrownBy(() ->
                resolver.resolve(
                        DocumentArea.GENERAL,
                        "contracts/\u0000sample.pdf"
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("制御文字");
    }
}
