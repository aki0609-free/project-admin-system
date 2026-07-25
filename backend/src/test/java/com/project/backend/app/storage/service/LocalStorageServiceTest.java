package com.project.backend.app.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.project.backend.app.storage.model.StorageListPage;
import com.project.backend.app.storage.properties.StorageProperties;

class LocalStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setLocalBasePath(temporaryDirectory.toString());
        storageService = new LocalStorageService(properties);
    }

    @Test
    void listDirectory_shouldReturnFoldersBeforeFilesWithPagination()
            throws Exception {
        storageService.createDirectory("documents/general/contracts");
        save("documents/general/b.txt", "b");
        save("documents/general/a.txt", "a");

        StorageListPage firstPage = storageService.listDirectory(
                "documents/general",
                null,
                2
        );

        assertThat(firstPage.entries())
                .extracting(entry -> entry.name())
                .containsExactly("contracts", "a.txt");
        assertThat(firstPage.truncated()).isTrue();
        assertThat(firstPage.nextContinuationToken()).isEqualTo("2");

        StorageListPage secondPage = storageService.listDirectory(
                "documents/general",
                firstPage.nextContinuationToken(),
                2
        );

        assertThat(secondPage.entries())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.name()).isEqualTo("b.txt");
                    assertThat(entry.directory()).isFalse();
                });
        assertThat(secondPage.truncated()).isFalse();
    }

    @Test
    void copy_shouldCreateParentDirectoriesAndPreserveContent()
            throws Exception {
        save("documents/general/source.txt", "source");

        storageService.copy(
                "documents/general/source.txt",
                "documents/general/archive/copied.txt"
        );

        assertThat(Files.readString(
                temporaryDirectory.resolve(
                        "documents/general/archive/copied.txt"
                )
        )).isEqualTo("source");
    }

    @Test
    void listRecursively_shouldReturnNestedFilesAndDirectories() {
        storageService.createDirectory(
                "documents/general/contracts/2026"
        );
        save("documents/general/contracts/2026/sample.pdf", "pdf");

        assertThat(storageService.listRecursively(
                "documents/general/contracts"
        ))
                .extracting(entry -> entry.key())
                .containsExactly(
                        "documents/general/contracts/2026",
                        "documents/general/contracts/2026/sample.pdf"
                );
    }

    @Test
    void save_shouldRejectPathTraversal() {
        assertThatThrownBy(() -> save("../outside.txt", "outside"))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseMessage(
                        "不正なファイルキーです。 key=../outside.txt"
                );
    }

    private void save(String key, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        storageService.save(
                key,
                new ByteArrayInputStream(bytes),
                bytes.length,
                "text/plain"
        );
    }
}
