package com.project.backend.features.admin.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.admin.document.dto.DocumentDownload;
import com.project.backend.features.admin.document.dto.DocumentEntryResponse;
import com.project.backend.features.admin.document.dto.DocumentListResponse;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerDownload;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerItemRequest;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerRequest;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerResponse;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.exception.DocumentOperationNotAllowedException;

class SyncfusionFileManagerServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-27T12:00:00Z");

    private DocumentManagementService documentService;
    private SyncfusionFileManagerService service;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentManagementService.class);
        service = new SyncfusionFileManagerService(
                documentService,
                new DocumentAreaPolicy(),
                Clock.fixed(NOW, ZoneId.of("Asia/Tokyo"))
        );
    }

    @Test
    void read_shouldConvertEntriesAndExposeEditablePermissionForGeneral() {
        when(documentService.list(
                DocumentArea.GENERAL,
                "",
                null,
                1000
        )).thenReturn(new DocumentListResponse(
                List.of(
                        entry("contracts", true, 0L),
                        entry("manual.pdf", false, 120L)
                ),
                null,
                false
        ));

        SyncfusionFileManagerResponse response = service.execute(
                DocumentArea.GENERAL,
                request("read", "/")
        );

        assertThat(response.cwd().name()).isEqualTo("会社書類");
        assertThat(response.cwd().permission().upload()).isTrue();
        assertThat(response.files())
                .extracting("name")
                .containsExactly("contracts", "manual.pdf");
        assertThat(response.files().getFirst().hasChild()).isTrue();
        assertThat(response.files().getLast().type()).isEqualTo(".pdf");
    }

    @Test
    void read_shouldExposeReadOnlyPermissionForManagedArea() {
        when(documentService.list(
                DocumentArea.BACKUPS,
                "",
                null,
                1000
        )).thenReturn(new DocumentListResponse(
                List.of(entry("2025", true, 0L)),
                null,
                false
        ));

        SyncfusionFileManagerResponse response = service.execute(
                DocumentArea.BACKUPS,
                request("read", "/")
        );

        assertThat(response.cwd().permission().write()).isFalse();
        assertThat(response.cwd().permission().upload()).isFalse();
        assertThat(response.cwd().permission().download()).isTrue();
    }

    @Test
    void create_shouldDelegateToExistingDocumentService() {
        when(documentService.list(
                DocumentArea.GENERAL,
                "contracts",
                null,
                1000
        )).thenReturn(new DocumentListResponse(
                List.of(entry("contracts/2026", true, 0L)),
                null,
                false
        ));

        SyncfusionFileManagerRequest request =
                new SyncfusionFileManagerRequest(
                        "create",
                        "/contracts/",
                        "2026",
                        null,
                        List.of(),
                        null,
                        null,
                        List.of(),
                        List.of()
                );

        SyncfusionFileManagerResponse response = service.execute(
                DocumentArea.GENERAL,
                request
        );

        verify(documentService).createDirectory(
                DocumentArea.GENERAL,
                "contracts/2026"
        );
        assertThat(response.files())
                .singleElement()
                .extracting("name")
                .isEqualTo("2026");
    }

    @Test
    void create_shouldBeRejectedForReadOnlyArea() {
        SyncfusionFileManagerRequest request =
                new SyncfusionFileManagerRequest(
                        "create",
                        "/",
                        "manual",
                        null,
                        List.of(),
                        null,
                        null,
                        List.of(),
                        List.of()
                );

        assertThatThrownBy(() ->
                service.execute(DocumentArea.TEMPLATES, request))
                .isInstanceOf(DocumentOperationNotAllowedException.class);
    }

    @Test
    void download_shouldStreamSingleFileWithoutArchive()
            throws Exception {
        byte[] content = "sample".getBytes();
        when(documentService.list(
                DocumentArea.GENERAL,
                "",
                null,
                1000
        )).thenReturn(new DocumentListResponse(
                List.of(entry("manual.pdf", false, content.length)),
                null,
                false
        ));
        when(documentService.download(
                DocumentArea.GENERAL,
                "manual.pdf"
        )).thenReturn(new DocumentDownload(
                "manual.pdf",
                "application/pdf",
                new ByteArrayInputStream(content)
        ));

        SyncfusionFileManagerDownload download = service.download(
                DocumentArea.GENERAL,
                new SyncfusionFileManagerRequest(
                        "download",
                        "/",
                        null,
                        null,
                        List.of("manual.pdf"),
                        null,
                        null,
                        List.of(),
                        List.of(new SyncfusionFileManagerItemRequest(
                                "manual.pdf",
                                true,
                                "/"
                        ))
                )
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        download.body().writeTo(output);

        assertThat(download.fileName()).isEqualTo("manual.pdf");
        assertThat(download.contentType()).isEqualTo("application/pdf");
        assertThat(output.toByteArray()).isEqualTo(content);
    }

    @Test
    void download_shouldCreateZipForDirectory()
            throws Exception {
        when(documentService.list(
                DocumentArea.BACKUPS,
                "reports",
                null,
                1000
        )).thenReturn(new DocumentListResponse(
                List.of(entry("reports/2025", true, 0L)),
                null,
                false
        ));
        when(documentService.listRecursively(
                DocumentArea.BACKUPS,
                "reports/2025"
        )).thenReturn(List.of(
                entry("reports/2025/payroll.pdf", false, 3L)
        ));
        when(documentService.download(
                DocumentArea.BACKUPS,
                "reports/2025/payroll.pdf"
        )).thenReturn(new DocumentDownload(
                "payroll.pdf",
                "application/pdf",
                new ByteArrayInputStream(new byte[] {1, 2, 3})
        ));

        SyncfusionFileManagerDownload download = service.download(
                DocumentArea.BACKUPS,
                new SyncfusionFileManagerRequest(
                        "download",
                        "/reports/",
                        null,
                        null,
                        List.of("2025"),
                        null,
                        null,
                        List.of(),
                        List.of(new SyncfusionFileManagerItemRequest(
                                "2025",
                                false,
                                "/reports/"
                        ))
                )
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        download.body().writeTo(output);

        try (ZipInputStream zipInputStream = new ZipInputStream(
                new ByteArrayInputStream(output.toByteArray())
        )) {
            assertThat(zipInputStream.getNextEntry().getName())
                    .isEqualTo("2025/");
            assertThat(zipInputStream.getNextEntry().getName())
                    .isEqualTo("2025/payroll.pdf");
        }
    }

    @Test
    void search_shouldTreatWildcardOnlyAsRecursiveList() {
        when(documentService.listRecursively(
                DocumentArea.GENERAL,
                "contracts"
        )).thenReturn(List.of(
                entry("contracts/2026/sample.pdf", false, 3L)
        ));

        SyncfusionFileManagerResponse response = service.execute(
                DocumentArea.GENERAL,
                new SyncfusionFileManagerRequest(
                        "search",
                        "/contracts/",
                        null,
                        null,
                        List.of(),
                        null,
                        "*",
                        List.of(),
                        List.of()
                )
        );

        verify(documentService).listRecursively(
                DocumentArea.GENERAL,
                "contracts"
        );
        assertThat(response.files())
                .singleElement()
                .extracting("name")
                .isEqualTo("sample.pdf");
    }

    @Test
    void download_shouldResolveDirectoryFromStorageWhenRequestDataIsMissing()
            throws Exception {
        when(documentService.list(
                DocumentArea.GENERAL,
                "",
                null,
                1000
        )).thenReturn(new DocumentListResponse(
                List.of(entry("contracts", true, 0L)),
                null,
                false
        ));
        when(documentService.listRecursively(
                DocumentArea.GENERAL,
                "contracts"
        )).thenReturn(List.of());

        SyncfusionFileManagerDownload download = service.download(
                DocumentArea.GENERAL,
                new SyncfusionFileManagerRequest(
                        "download",
                        "/",
                        null,
                        null,
                        List.of("contracts"),
                        null,
                        null,
                        List.of(),
                        List.of()
                )
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        download.body().writeTo(output);

        assertThat(download.fileName()).isEqualTo("contracts.zip");
        try (ZipInputStream zipInputStream = new ZipInputStream(
                new ByteArrayInputStream(output.toByteArray())
        )) {
            assertThat(zipInputStream.getNextEntry().getName())
                    .isEqualTo("contracts/");
        }
    }

    private SyncfusionFileManagerRequest request(
            String action,
            String path
    ) {
        return new SyncfusionFileManagerRequest(
                action,
                path,
                null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                List.of()
        );
    }

    private DocumentEntryResponse entry(
            String path,
            boolean directory,
            long size
    ) {
        String name = path.contains("/")
                ? path.substring(path.lastIndexOf("/") + 1)
                : path;
        return new DocumentEntryResponse(
                path,
                name,
                directory,
                size,
                NOW,
                "etag"
        );
    }
}
