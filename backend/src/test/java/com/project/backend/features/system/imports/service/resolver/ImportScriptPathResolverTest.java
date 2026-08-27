package com.project.backend.features.system.imports.service.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.imports.properties.ImportScriptProperties;

class ImportScriptPathResolverTest {

    @TempDir
    Path tempDirectory;

    @Test
    void resolve_shouldUseConfiguredLocalDirectoryDirectly() throws Exception {
        StorageProperties storageProperties = properties();
        StorageService storageService = mock(StorageService.class);
        when(storageService.type()).thenReturn(StorageType.LOCAL);
        Path script = tempDirectory.resolve("imports/scripts/sample.py");
        Files.createDirectories(script.getParent());
        Files.writeString(script, "print('ok')", StandardCharsets.UTF_8);

        ImportScriptPathResolver resolver = new ImportScriptPathResolver(
                storageProperties,
                storageService,
                scriptProperties()
        );

        try (ResolvedImportScript result = resolver.resolve("sample.py")) {
            assertThat(result.path()).isEqualTo(script.toAbsolutePath());
        }

        assertThat(script).exists();
    }

    @Test
    void resolve_shouldMaterializeS3ScriptAndDeleteTemporaryFileOnClose()
            throws Exception {
        StorageProperties storageProperties = properties();
        StorageService storageService = mock(StorageService.class);
        when(storageService.type()).thenReturn(StorageType.S3);
        when(storageService.exists(StorageType.S3, "imports/scripts/sample.py"))
                .thenReturn(true);
        when(storageService.load(StorageType.S3, "imports/scripts/sample.py"))
                .thenReturn(new ByteArrayInputStream(
                        "print('s3')".getBytes(StandardCharsets.UTF_8)
                ));
        ImportScriptProperties scriptProperties = scriptProperties();

        ImportScriptPathResolver resolver = new ImportScriptPathResolver(
                storageProperties,
                storageService,
                scriptProperties
        );

        Path materialized;
        try (ResolvedImportScript result = resolver.resolve(
                "imports/scripts/sample.py"
        )) {
            materialized = result.path();
            assertThat(materialized).hasContent("print('s3')");
        }

        assertThat(materialized).doesNotExist();
    }

    @Test
    void resolve_shouldRejectTraversalAndUnsupportedFile() {
        StorageService storageService = mock(StorageService.class);
        when(storageService.type()).thenReturn(StorageType.LOCAL);
        ImportScriptPathResolver resolver = new ImportScriptPathResolver(
                properties(),
                storageService,
                scriptProperties()
        );

        assertThatThrownBy(() -> resolver.resolve("../secret.py"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不正なscriptPath");
        assertThatThrownBy(() -> resolver.resolve("sample.txt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(".pyまたは.sh");
    }

    private StorageProperties properties() {
        StorageProperties properties = new StorageProperties();
        properties.setLocalBasePath(tempDirectory.toString());
        return properties;
    }

    private ImportScriptProperties scriptProperties() {
        ImportScriptProperties properties = new ImportScriptProperties();
        properties.setWorkDirectory(
                tempDirectory.resolve("work").toString()
        );
        return properties;
    }
}
