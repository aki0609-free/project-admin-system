package com.project.backend.app.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.properties.StorageProperties;

class StorageServiceTest {

    private StorageBackend localBackend;
    private StorageBackend s3Backend;
    private StorageProperties properties;
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        localBackend = backend(StorageType.LOCAL);
        s3Backend = backend(StorageType.S3);
        properties = new StorageProperties();
        storageService = new StorageService(
                List.of(localBackend, s3Backend),
                properties
        );
        clearInvocations(localBackend, s3Backend);
    }

    @Test
    void load_shouldRouteToLocalBackend_whenStorageTypeIsLocal() {
        InputStream expected = new ByteArrayInputStream(new byte[] {1});
        when(localBackend.load("reports/sample.pdf")).thenReturn(expected);

        InputStream actual = storageService.load(
                StorageType.LOCAL,
                "reports/sample.pdf"
        );

        assertThat(actual).isSameAs(expected);
        verify(localBackend).load("reports/sample.pdf");
        verifyNoInteractions(s3Backend);
    }

    @Test
    void load_shouldRouteToS3Backend_whenStorageTypeIsS3() {
        InputStream expected = new ByteArrayInputStream(new byte[] {2});
        when(s3Backend.load("reports/sample.pdf")).thenReturn(expected);

        InputStream actual = storageService.load(
                StorageType.S3,
                "reports/sample.pdf"
        );

        assertThat(actual).isSameAs(expected);
        verify(s3Backend).load("reports/sample.pdf");
        verifyNoInteractions(localBackend);
    }

    @Test
    void save_shouldUseConfiguredDefaultBackend() {
        properties.setDefaultType(StorageType.S3);
        InputStream inputStream = new ByteArrayInputStream(new byte[] {1, 2, 3});
        when(s3Backend.save("documents/file.pdf", inputStream, 3, "application/pdf"))
                .thenReturn("documents/file.pdf");

        String key = storageService.save(
                "documents/file.pdf",
                inputStream,
                3,
                "application/pdf"
        );

        assertThat(key).isEqualTo("documents/file.pdf");
        verify(s3Backend).save(
                "documents/file.pdf",
                inputStream,
                3,
                "application/pdf"
        );
        verifyNoInteractions(localBackend);
    }

    @Test
    void exists_shouldUseLegacyType_whenDefaultTypeIsNotConfigured() {
        properties.setType(StorageType.S3);
        when(s3Backend.exists("legacy/file.pdf")).thenReturn(true);

        boolean exists = storageService.exists("legacy/file.pdf");

        assertThat(exists).isTrue();
        verify(s3Backend).exists("legacy/file.pdf");
    }

    @Test
    void type_shouldDefaultToLocal_whenNoTypeIsConfigured() {
        assertThat(storageService.type()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    void load_shouldRejectNullStorageType() {
        assertThatThrownBy(() -> storageService.load(null, "file.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storageType");
    }

    @Test
    void load_shouldFailClearly_whenRequestedBackendIsDisabled() {
        StorageService localOnlyService = new StorageService(
                List.of(localBackend),
                properties
        );

        assertThatThrownBy(() -> localOnlyService.load(StorageType.S3, "file.pdf"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3");
    }

    @Test
    void constructor_shouldRejectDuplicateBackendType() {
        StorageBackend anotherLocalBackend = backend(StorageType.LOCAL);

        assertThatThrownBy(() -> new StorageService(
                List.of(localBackend, anotherLocalBackend),
                properties
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOCAL");
    }

    private StorageBackend backend(StorageType storageType) {
        StorageBackend backend = mock(StorageBackend.class);
        when(backend.type()).thenReturn(storageType);
        return backend;
    }
}
