package com.project.backend.features.system.report.service.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.report.service.builder.ReportFileKeyBuilder;
import com.project.backend.features.system.report.service.validation.ReportStorageValidator;

class ReportStorageServiceTest {

    @Test
    void load_shouldUseStoredStorageType() {
        StorageService storageService = mock(StorageService.class);
        ReportFileKeyBuilder keyBuilder = mock(ReportFileKeyBuilder.class);
        ReportStorageValidator validator = mock(ReportStorageValidator.class);
        ReportStorageService service = new ReportStorageService(
                storageService,
                keyBuilder,
                validator
        );

        when(storageService.load(StorageType.LOCAL, "reports/old.pdf"))
                .thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));

        byte[] data = service.load(StorageType.LOCAL, "reports/old.pdf");

        assertThat(data).containsExactly(1, 2, 3);
        verify(storageService).load(StorageType.LOCAL, "reports/old.pdf");
    }

    @Test
    void exists_shouldUseStoredStorageType() {
        StorageService storageService = mock(StorageService.class);
        ReportStorageService service = new ReportStorageService(
                storageService,
                mock(ReportFileKeyBuilder.class),
                mock(ReportStorageValidator.class)
        );

        when(storageService.exists(StorageType.S3, "reports/new.pdf"))
                .thenReturn(true);

        assertThat(service.exists(StorageType.S3, "reports/new.pdf")).isTrue();
        verify(storageService).exists(StorageType.S3, "reports/new.pdf");
    }
}
