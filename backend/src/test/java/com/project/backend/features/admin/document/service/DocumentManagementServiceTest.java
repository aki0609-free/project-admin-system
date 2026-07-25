package com.project.backend.features.admin.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.project.backend.app.storage.model.StorageEntry;
import com.project.backend.app.storage.model.StorageListPage;
import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.dto.DocumentListResponse;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.exception.DocumentOperationNotAllowedException;

class DocumentManagementServiceTest {

    private StorageService storageService;
    private DocumentManagementService service;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        DocumentAreaPolicy policy = new DocumentAreaPolicy();
        DocumentStorageKeyResolver resolver =
                new DocumentStorageKeyResolver(
                        new StorageProperties()
                );

        service = new DocumentManagementService(
                storageService,
                policy,
                resolver
        );
    }

    @Test
    void list_shouldHidePhysicalRootFromResponse() {
        when(storageService.listDirectory(
                "documents/general/contracts",
                null,
                100
        )).thenReturn(new StorageListPage(
                List.of(new StorageEntry(
                        "documents/general/contracts/sample.pdf",
                        "sample.pdf",
                        false,
                        120L,
                        Instant.parse("2026-07-25T00:00:00Z"),
                        "etag"
                )),
                null,
                false
        ));

        DocumentListResponse result = service.list(
                DocumentArea.GENERAL,
                "contracts",
                null,
                100
        );

        assertThat(result.entries())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.path())
                            .isEqualTo("contracts/sample.pdf");
                    assertThat(entry.name()).isEqualTo("sample.pdf");
                    assertThat(entry.size()).isEqualTo(120L);
                });
    }

    @Test
    void upload_shouldBeAllowedOnlyInGeneralArea()
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                new byte[] {1, 2, 3}
        );

        service.upload(DocumentArea.GENERAL, "contracts", file);

        verify(storageService).save(
                org.mockito.ArgumentMatchers.eq(
                        "documents/general/contracts/sample.pdf"
                ),
                any(InputStream.class),
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq("application/pdf")
        );

        assertThatThrownBy(() ->
                service.upload(DocumentArea.BACKUPS, "", file))
                .isInstanceOf(DocumentOperationNotAllowedException.class)
                .hasMessageContaining("UPLOAD");
    }

    @Test
    void upload_shouldRejectPathCharactersInFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../outside.txt",
                "text/plain",
                new byte[] {1}
        );

        assertThatThrownBy(() ->
                service.upload(DocumentArea.GENERAL, "", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ファイル名が不正");

        verifyNoInteractions(storageService);
    }

    @Test
    void move_shouldNotDeleteSourceWhenCopyFails() {
        when(storageService.exists(
                "documents/general/source.txt"
        )).thenReturn(true);
        when(storageService.exists(
                "documents/general/target.txt"
        )).thenReturn(false);
        when(storageService.directoryExists(
                "documents/general/target.txt"
        )).thenReturn(false);
        doThrow(new RuntimeException("copy failed"))
                .when(storageService)
                .copy(
                        "documents/general/source.txt",
                        "documents/general/target.txt"
                );

        assertThatThrownBy(() ->
                service.move(
                        DocumentArea.GENERAL,
                        "source.txt",
                        "target.txt",
                        false
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("copy failed");

        verify(storageService, never()).delete(
                "documents/general/source.txt"
        );
    }

    @Test
    void delete_shouldRejectAreaRoot() {
        assertThatThrownBy(() ->
                service.delete(
                        DocumentArea.GENERAL,
                        "",
                        true
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ルート");

        verifyNoInteractions(storageService);
    }

    @Test
    void managedAreas_shouldRejectMutatingOperations() {
        assertThatThrownBy(() ->
                service.createDirectory(
                        DocumentArea.GENERATED_REPORTS,
                        "manual"
                ))
                .isInstanceOf(DocumentOperationNotAllowedException.class);
        assertThatThrownBy(() ->
                service.delete(
                        DocumentArea.TEMPLATES,
                        "ledgers",
                        true
                ))
                .isInstanceOf(DocumentOperationNotAllowedException.class);

        verifyNoInteractions(storageService);
    }
}
